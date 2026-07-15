package com.zoyluo.vitals.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.zoyluo.vitals.config.VitalsConfig;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class VitalsConfigManager {
	public static final String SAVE_ERROR_CODE = "VITALS-CONFIG-001";

	private static final Logger LOGGER = LoggerFactory.getLogger("Vitals");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("vitals.json");
	private static final Path BACKUP_PATH = CONFIG_PATH.resolveSibling("vitals.json.bak");
	private static final Path TEMP_PATH = CONFIG_PATH.resolveSibling("vitals.json.tmp");
	private static final DateTimeFormatter CORRUPT_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	private static VitalsConfig current = VitalsConfig.defaults();

	private VitalsConfigManager() {
	}

	public static synchronized void load() {
		if (!Files.exists(CONFIG_PATH)) {
			current = VitalsConfig.defaults();
			SaveResult result = saveAndApply(current);
			if (!result.success()) {
				LOGGER.warn("[{}] Could not create the default Vitals config at {}", SAVE_ERROR_CODE, CONFIG_PATH);
			}
			return;
		}

		try {
			current = read(CONFIG_PATH);
		} catch (IOException | JsonParseException exception) {
			LOGGER.warn("[VITALS-CONFIG-002] Invalid Vitals config at {}; preserving it and trying the backup", CONFIG_PATH, exception);
			preserveCorruptConfig();
			current = readBackupOrDefaults();
			// Never copy a known-corrupt primary file over the last usable backup.
			SaveResult recoveryResult = saveAndApply(current, false);
			if (!recoveryResult.success()) {
				LOGGER.error("[{}] Recovered Vitals settings in memory but could not restore {}", recoveryResult.errorCode(), CONFIG_PATH);
			}
		}
	}

	public static synchronized VitalsConfig current() {
		return current;
	}

	public static synchronized SaveResult saveAndApply(VitalsConfig candidate) {
		return saveAndApply(candidate, true);
	}

	private static SaveResult saveAndApply(VitalsConfig candidate, boolean backupExisting) {
		VitalsConfig normalized = candidate.sanitizedCopy();
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			writeAndForce(TEMP_PATH, GSON.toJson(normalized) + System.lineSeparator());
			VitalsConfig verified = read(TEMP_PATH);

			if (backupExisting && Files.exists(CONFIG_PATH)) {
				Files.copy(CONFIG_PATH, BACKUP_PATH, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			}
			moveIntoPlace(TEMP_PATH, CONFIG_PATH);
			current = verified;
			return SaveResult.ok();
		} catch (IOException | JsonParseException exception) {
			LOGGER.error("[{}] Failed to save Vitals config to {}", SAVE_ERROR_CODE, CONFIG_PATH, exception);
			try {
				Files.deleteIfExists(TEMP_PATH);
			} catch (IOException cleanupException) {
				LOGGER.warn("[VITALS-CONFIG-003] Failed to remove temporary config {}", TEMP_PATH, cleanupException);
			}
			return SaveResult.failed(SAVE_ERROR_CODE);
		}
	}

	private static VitalsConfig read(Path path) throws IOException, JsonParseException {
		VitalsConfig loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), VitalsConfig.class);
		if (loaded == null) {
			throw new JsonParseException("Config root must be an object");
		}
		return loaded.sanitizedCopy();
	}

	private static void writeAndForce(Path path, String contents) throws IOException {
		byte[] bytes = contents.getBytes(StandardCharsets.UTF_8);
		try (FileChannel channel = FileChannel.open(
				path,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
		)) {
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			while (buffer.hasRemaining()) {
				channel.write(buffer);
			}
			channel.force(true);
		}
	}

	private static void moveIntoPlace(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private static void preserveCorruptConfig() {
		if (!Files.exists(CONFIG_PATH)) {
			return;
		}
		String timestamp = CORRUPT_TIMESTAMP.format(LocalDateTime.now());
		Path corruptPath = CONFIG_PATH.resolveSibling("vitals.json.corrupt-" + timestamp);
		try {
			Files.move(CONFIG_PATH, corruptPath, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.warn("[VITALS-CONFIG-002] Preserved invalid config as {}", corruptPath);
		} catch (IOException exception) {
			LOGGER.error("[VITALS-CONFIG-004] Failed to preserve invalid config {}", CONFIG_PATH, exception);
		}
	}

	private static VitalsConfig readBackupOrDefaults() {
		if (Files.exists(BACKUP_PATH)) {
			try {
				VitalsConfig recovered = read(BACKUP_PATH);
				LOGGER.warn("[VITALS-CONFIG-002] Recovered Vitals settings from {}", BACKUP_PATH);
				return recovered;
			} catch (IOException | JsonParseException exception) {
				LOGGER.error("[VITALS-CONFIG-005] Vitals backup is also invalid: {}", BACKUP_PATH, exception);
			}
		}
		return VitalsConfig.defaults();
	}

	public record SaveResult(boolean success, String errorCode) {
		private static SaveResult ok() {
			return new SaveResult(true, "");
		}

		private static SaveResult failed(String errorCode) {
			return new SaveResult(false, errorCode);
		}
	}
}
