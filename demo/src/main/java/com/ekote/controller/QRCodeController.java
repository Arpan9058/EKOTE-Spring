package com.ekote.controller;

import com.ekote.repositories.GunInfoRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/qrcode")
public class QRCodeController {

  private final GunInfoRepository gunInfoRepository;
  private final String publicBaseUrl;

  public QRCodeController(
      GunInfoRepository gunInfoRepository,
      @Value("${ekote.public-base-url:http://localhost:8080}") String publicBaseUrl) {
    URI uri = URI.create(publicBaseUrl);
    if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null) {
      throw new IllegalArgumentException(
          "ekote.public-base-url must be an HTTP(S) URL without credentials, query or fragment");
    }
    this.gunInfoRepository = gunInfoRepository;
    this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
  }

  @GetMapping(produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> generateQRCode(@RequestParam String gunId) {
    boolean exists = gunInfoRepository.existsByUniqueIdentifier(gunId);
    if (!exists) {
      return ResponseEntity.badRequest().body(null);
    }

    String qrCodeContent =
        UriComponentsBuilder.fromUriString(publicBaseUrl)
            .pathSegment("gunDetails", gunId)
            .build()
            .encode()
            .toUriString();

    try {
      byte[] qrImage = getQRCodeImage(qrCodeContent, 400, 400);
      return ResponseEntity.ok(qrImage);
    } catch (Exception e) {
      return ResponseEntity.internalServerError().build();
    }
  }

  private byte[] getQRCodeImage(String text, int width, int height) throws Exception {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix matrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
    return outputStream.toByteArray();
  }
}
