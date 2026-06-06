import 'dart:convert';
import 'dart:typed_data';

import 'package:asn1lib/asn1lib.dart';
import 'package:pointycastle/api.dart';
import 'package:pointycastle/asymmetric/api.dart';
import 'package:pointycastle/asymmetric/oaep.dart';
import 'package:pointycastle/asymmetric/rsa.dart';

class RsaOaep {
  static String encryptSpkiBase64(String publicKeyBase64, String plaintext) {
    final publicKey = _parseSpkiPublicKey(publicKeyBase64);
    final cipher = OAEPEncoding.withSHA256(RSAEngine())
      ..init(true, PublicKeyParameter<RSAPublicKey>(publicKey));
    final input = Uint8List.fromList(utf8.encode(plaintext));
    final encrypted = cipher.process(input);
    return base64Encode(encrypted);
  }

  static RSAPublicKey _parseSpkiPublicKey(String publicKeyBase64) {
    final der = base64Decode(publicKeyBase64);
    final top = ASN1Parser(Uint8List.fromList(der)).nextObject();
    if (top is! ASN1Sequence || top.elements.length < 2) {
      throw const FormatException('公钥格式无效');
    }

    final bitString = top.elements[1];
    if (bitString is! ASN1BitString) {
      throw const FormatException('公钥内容无效');
    }

    final rsaObject = ASN1Parser(bitString.contentBytes()).nextObject();
    if (rsaObject is! ASN1Sequence || rsaObject.elements.length < 2) {
      throw const FormatException('RSA 公钥结构无效');
    }

    final modulus = rsaObject.elements[0];
    final exponent = rsaObject.elements[1];
    if (modulus is! ASN1Integer || exponent is! ASN1Integer) {
      throw const FormatException('RSA 公钥参数无效');
    }

    return RSAPublicKey(modulus.valueAsBigInteger, exponent.valueAsBigInteger);
  }
}
