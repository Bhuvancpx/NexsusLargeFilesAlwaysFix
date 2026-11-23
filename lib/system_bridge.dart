import 'package:flutter/services.dart';

class NexsusSystemBridge {
  NexsusSystemBridge({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(channelName);

  static const channelName = 'com.nexsusai.app/system';
  final MethodChannel _channel;

  Future<Map<String, dynamic>> getDeviceInfo() async {
    final result =
        await _channel.invokeMapMethod<String, dynamic>('getDeviceInfo');
    if (result == null) {
      throw StateError('Android returned no device information.');
    }
    return result;
  }

  Future<List<Map<String, dynamic>>> scanLargeFiles() async {
    final result = await _channel.invokeListMethod<Object?>('scanLargeFiles');
    if (result == null) {
      throw StateError('Android returned no media scan result.');
    }
    return result
        .map((item) => Map<String, dynamic>.from(item! as Map))
        .toList(growable: false);
  }

  Future<bool> openScannedFile(String uri, String? mimeType) async =>
      await _channel.invokeMethod<bool>('openScannedFile', {
        'uri': uri,
        'mimeType': mimeType,
      }) ??
      false;

  Future<bool> openStorageSettings() async =>
      await _channel.invokeMethod<bool>('openStorageSettings') ?? false;

  Future<bool> setAlarm(int hour, int minute) async =>
      await _channel.invokeMethod<bool>('setAlarm', {
        'hour': hour,
        'minute': minute,
      }) ??
      false;

  Future<bool> cancelAlarm() async =>
      await _channel.invokeMethod<bool>('cancelAlarm') ?? false;

  Future<bool> openExactAlarmSettings() async =>
      await _channel.invokeMethod<bool>('openExactAlarmSettings') ?? false;

  Future<bool> openAodSettings() async =>
      await _channel.invokeMethod<bool>('openAodSettings') ?? false;

  Future<Map<String, dynamic>> startAod() async {
    final result = await _channel.invokeMapMethod<String, dynamic>(
      'startNexsusAod',
      const {
        'clockStyle': 0,
        'clockStyleName': 'Digital',
        'use24Hour': false,
        'showDate': true,
        'showBattery': true,
        'showNexsus': true,
        'brightness': 0.7,
      },
    );
    if (result == null) {
      throw StateError('Android returned no AOD service result.');
    }
    return result;
  }

  Future<bool> stopAod() async =>
      await _channel.invokeMethod<bool>('stopNexsusAod') ?? false;

  Future<bool> openAodPermission() async =>
      await _channel.invokeMethod<bool>('openAodPermission') ?? false;

  Future<bool> openDisplaySettings() async =>
      await _channel.invokeMethod<bool>('openDisplaySettings') ?? false;

  Future<bool> openNotificationAccess() async =>
      await _channel.invokeMethod<bool>('openNotificationAccess') ?? false;

  Future<bool> isNotificationAccessEnabled() async =>
      await _channel.invokeMethod<bool>('isNotificationAccessEnabled') ??
      false;
}
