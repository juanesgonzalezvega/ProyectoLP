package com.example.proyectolp;

import com.example.proyectolp.model.enums.AttachmentKind;
import com.example.proyectolp.model.enums.AttachmentOwnerType;
import com.example.proyectolp.service.AttachmentRules;
import com.example.proyectolp.service.FileSignatureChecker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentRulesTest {
    @TempDir Path temp;

    @Test void extensionSizeAndCountRulesAreEnforced() {
        assertEquals(AttachmentKind.imagen, AttachmentRules.kindForExtension("PNG"));
        assertEquals(AttachmentKind.audio, AttachmentRules.kindForExtension("m4a"));
        assertEquals(AttachmentKind.video, AttachmentRules.kindForExtension("MP4"));
        assertThrows(IllegalArgumentException.class, () -> AttachmentRules.kindForExtension("pdf"));
        assertDoesNotThrow(() -> AttachmentRules.validateSize(AttachmentKind.imagen, AttachmentRules.IMAGE_MAX_BYTES));
        assertThrows(IllegalArgumentException.class, () -> AttachmentRules.validateSize(AttachmentKind.imagen, AttachmentRules.IMAGE_MAX_BYTES + 1));
        assertDoesNotThrow(() -> AttachmentRules.validateCount(9));
        assertThrows(IllegalArgumentException.class, () -> AttachmentRules.validateCount(10));
    }

    @Test void sanitizesNamesAndBuildsOpaquePaths() {
        assertEquals("x.png", AttachmentRules.sanitizeName("../../x.png"));
        assertEquals("archivo", AttachmentRules.sanitizeName("../\u0000"));
        assertEquals(120, AttachmentRules.sanitizeName("a".repeat(140)).length());
        UUID id = UUID.randomUUID();
        String path = AttachmentRules.storagePath("12345678-1234-1234-1234-123456789012", AttachmentOwnerType.TASK, 9, id, "PNG");
        assertEquals("12345678-1234-1234-1234-123456789012/task/9/" + id + ".png", path);
        assertFalse(path.contains("original"));
    }

    @Test void checksRealSignaturesAndRejectsRenamedOrTruncatedFiles() throws Exception {
        Path png = temp.resolve("photo.png");
        Files.write(png, new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});
        assertDoesNotThrow(() -> FileSignatureChecker.validate(png, "png"));
        Path fake = temp.resolve("fake.png");
        Files.write(fake, "MZ executable".getBytes());
        assertThrows(IllegalArgumentException.class, () -> FileSignatureChecker.validate(fake, "png"));
        Path mp4 = temp.resolve("short.mp4");
        Files.write(mp4, new byte[]{0, 0, 0, 0, 'f', 't', 'y', 'p'});
        assertThrows(IllegalArgumentException.class, () -> FileSignatureChecker.validate(mp4, "mp4"));
    }
}
