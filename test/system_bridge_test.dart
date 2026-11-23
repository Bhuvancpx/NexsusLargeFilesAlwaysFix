import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:nexsus_android_test/system_bridge.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel(NexsusSystemBridge.channelName);
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  tearDown(() {
    messenger.setMockMethodCallHandler(channel, null);
  });

  test('reads device info from the system channel', () async {
    MethodCall? capturedCall;
    messenger.setMockMethodCallHandler(channel, (call) async {
      capturedCall = call;
      return {'battery': 82, 'deviceName': 'Test phone'};
    });

    final result = await NexsusSystemBridge(channel: channel).getDeviceInfo();

    expect(capturedCall?.method, 'getDeviceInfo');
    expect(result['battery'], 82);
    expect(result['deviceName'], 'Test phone');
  });

  test('scans media and opens a selected item with its URI and MIME type',
      () async {
    final calls = <MethodCall>[];
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      if (call.method == 'scanLargeFiles') {
        return [
          {
            'name': 'sample.mp4',
            'type': 'video',
            'size': 12000000,
            'uri': 'content://media/sample',
            'mimeType': 'video/mp4',
          }
        ];
      }
      return true;
    });
    final bridge = NexsusSystemBridge(channel: channel);

    final files = await bridge.scanLargeFiles();
    await bridge.openScannedFile(
      files.single['uri'] as String,
      files.single['mimeType'] as String,
    );

    expect(calls.map((call) => call.method),
        ['scanLargeFiles', 'openScannedFile']);
    expect(calls.last.arguments, {
      'uri': 'content://media/sample',
      'mimeType': 'video/mp4',
    });
  });

  test('passes alarm time to Android', () async {
    MethodCall? capturedCall;
    messenger.setMockMethodCallHandler(channel, (call) async {
      capturedCall = call;
      return true;
    });

    await NexsusSystemBridge(channel: channel).setAlarm(7, 45);

    expect(capturedCall?.method, 'setAlarm');
    expect(capturedCall?.arguments, {'hour': 7, 'minute': 45});
  });
}
