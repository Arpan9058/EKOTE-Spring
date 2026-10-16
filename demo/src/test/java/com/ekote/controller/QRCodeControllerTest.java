package com.ekote.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ekote.repositories.GunInfoRepository;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QRCodeControllerTest {
  private final GunInfoRepository repository = mock(GunInfoRepository.class);

  @Test
  void pngRoundTripUsesConfiguredOriginAndEncodesIdentifier() throws Exception {
    when(repository.existsByUniqueIdentifier("ITEM A?x=1")).thenReturn(true);
    var controller = new QRCodeController(repository, "https://inventory.example.test/base/");
    var response = controller.generateQRCode("ITEM A?x=1");
    assertEquals(200, response.getStatusCode().value());
    var image = ImageIO.read(new ByteArrayInputStream(response.getBody()));
    assertEquals(400, image.getWidth());
    assertEquals(400, image.getHeight());
    var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)));
    assertEquals(
        "https://inventory.example.test/base/gunDetails/ITEM%20A%3Fx=1",
        new MultiFormatReader().decode(bitmap).getText());
  }

  @Test
  void missingInventoryDoesNotProduceQrCode() {
    var response =
        new QRCodeController(repository, "http://localhost:8080").generateQRCode("missing");
    assertEquals(400, response.getStatusCode().value());
    assertNull(response.getBody());
  }

  @Test
  void rejectsUnsafeOrAmbiguousDestinations() {
    for (String destination :
        new String[] {
          "javascript:alert(1)",
          "/relative",
          "https://user:secret@example.test",
          "https://example.test?q=1",
          "https://example.test#fragment"
        }) {
      assertThrows(
          IllegalArgumentException.class, () -> new QRCodeController(repository, destination));
    }
  }
}
