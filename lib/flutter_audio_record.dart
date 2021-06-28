import 'dart:async';

import 'package:flutter/services.dart';

class FlutterAudioRecord {
  static const BasicMessageChannel _channel =
  const BasicMessageChannel('flutter_audio_record', StringCodec());

  // static Future<String?> get platformVersion async {
  //   final String? version = await _channel.invokeMethod('getPlatformVersion');
  //   return version;
  // }

  static void startRecord(callback) async {
    print('startRecord');
    //使用BasicMessageChannel接受来自Native的消息，并向Native回复
    _channel.setMessageHandler((message) async {
      // print("收到Native的消息：" + message);
      callback(message);
      // return Future<String>(() {
      //   return "收到Native的消息：" + message;
      // });
    });
    //使用BasicMessageChannel向Native发送消息，并接受Native的回复
    String response;
    try {
      response = await _channel.send("startRecord");
      print(response);
    } on PlatformException catch (e) {
      print(e);
    }
  }

  static void stopRecord() async {
    try {
      await _channel.send("stopRecord");
    } on PlatformException catch (e) {
      print(e);
    }
  }
}
