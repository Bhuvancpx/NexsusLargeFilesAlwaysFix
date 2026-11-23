import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'system_bridge.dart';

void main() {
  runApp(const NexsusTestApp());
}

class NexsusTestApp extends StatelessWidget {
  const NexsusTestApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Nexsus Android test app',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff5457d5)),
          useMaterial3: true,
        ),
        home: const SystemToolsPage(),
      );
}

class SystemToolsPage extends StatefulWidget {
  const SystemToolsPage({super.key});

  @override
  State<SystemToolsPage> createState() => _SystemToolsPageState();
}

class _SystemToolsPageState extends State<SystemToolsPage> {
  final _bridge = NexsusSystemBridge();
  Map<String, dynamic>? _deviceInfo;
  List<Map<String, dynamic>> _largeFiles = const [];
  String? _lastResult;
  String? _error;
  bool _busy = false;

  Future<void> _run(Future<Object?> Function() operation) async {
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      final result = await operation();
      if (!mounted) return;
      setState(() => _lastResult = result?.toString() ?? 'Completed.');
    } on PlatformException catch (error) {
      if (!mounted) return;
      setState(() => _error = '${error.code}: ${error.message ?? 'Failed'}');
    } catch (error) {
      if (!mounted) return;
      setState(() => _error = error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _refreshDeviceInfo() => _run(() async {
        final info = await _bridge.getDeviceInfo();
        if (mounted) setState(() => _deviceInfo = info);
        return info;
      });

  Future<void> _scanMedia() => _run(() async {
        final files = await _bridge.scanLargeFiles();
        if (mounted) setState(() => _largeFiles = files);
        return '${files.length} large media files found';
      });

  Future<void> _chooseAlarmTime() async {
    final time = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.now(),
    );
    if (time == null || !mounted) return;
    await _run(() => _bridge.setAlarm(time.hour, time.minute));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Nexsus Android test app')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'Run the native Android integrations on an emulator or your own '
            'device. Android may ask for permissions when a feature needs them.',
            style: Theme.of(context).textTheme.bodyLarge,
          ),
          const SizedBox(height: 16),
          _section(
            context,
            'Device',
            [
              _button('Read device information', _refreshDeviceInfo),
              if (_deviceInfo != null) _dataCard(_deviceInfo!),
            ],
          ),
          _section(
            context,
            'Large media files',
            [
              _button('Scan images, video, and audio', _scanMedia),
              _button(
                'Open storage settings',
                () => _run(_bridge.openStorageSettings),
              ),
              for (final file in _largeFiles)
                Card(
                  child: ListTile(
                    title: Text(file['name']?.toString() ?? 'Unknown file'),
                    subtitle: Text(
                      '${file['type']} · ${_formatBytes(file['size'])}',
                    ),
                    trailing: const Icon(Icons.open_in_new),
                    onTap: () => _run(
                      () => _bridge.openScannedFile(
                        file['uri'] as String,
                        file['mimeType'] as String?,
                      ),
                    ),
                  ),
                ),
            ],
          ),
          _section(
            context,
            'Alarm',
            [
              _button('Choose time and set alarm', _chooseAlarmTime),
              _button(
                'Open exact alarm permission settings',
                () => _run(_bridge.openExactAlarmSettings),
              ),
              _button('Cancel alarm', () => _run(_bridge.cancelAlarm)),
            ],
          ),
          _section(
            context,
            'Always-on display sample',
            [
              const Text(
                'This repository only starts a foreground-service sample. '
                'It does not keep the screen on or implement a true '
                'screen-off display. Some Android builds may require overlay '
                'permission before starting it.',
              ),
              _button(
                'Start sample service',
                () => _run(_bridge.startAod),
              ),
              _button('Stop sample service', () => _run(_bridge.stopAod)),
              _button(
                'Open overlay permission settings',
                () => _run(_bridge.openAodPermission),
              ),
              _button(
                'Open display settings',
                () => _run(_bridge.openDisplaySettings),
              ),
              _button(
                'Check notification access',
                () => _run(_bridge.isNotificationAccessEnabled),
              ),
              _button(
                'Open notification access settings',
                () => _run(_bridge.openNotificationAccess),
              ),
              const Text(
                'A notification-listener service is not included; notification '
                'access will normally remain disabled in this sample.',
              ),
            ],
          ),
          if (_busy)
            const Padding(
              padding: EdgeInsets.all(12),
              child: Center(child: CircularProgressIndicator()),
            ),
          if (_lastResult != null)
            _statusCard('Last result', _lastResult!, Colors.green),
          if (_error != null) _statusCard('Error', _error!, Colors.red),
        ],
      ),
    );
  }

  Widget _section(BuildContext context, String title, List<Widget> children) =>
      Card(
        margin: const EdgeInsets.only(bottom: 12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              ...children,
            ],
          ),
        ),
      );

  Widget _button(String label, VoidCallback onPressed) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Align(
          alignment: Alignment.centerLeft,
          child: OutlinedButton(
            onPressed: _busy ? null : onPressed,
            child: Text(label),
          ),
        ),
      );

  Widget _dataCard(Map<String, dynamic> data) => Card(
        color: Theme.of(context).colorScheme.surfaceContainerHighest,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Text(data.entries.map((entry) => '${entry.key}: ${entry.value}')
              .join('\n')),
        ),
      );

  Widget _statusCard(String title, String value, Color color) => Card(
        child: ListTile(
          title: Text(title, style: TextStyle(color: color)),
          subtitle: Text(value),
        ),
      );

  String _formatBytes(Object? value) {
    final bytes = value is num ? value.toDouble() : 0.0;
    if (bytes < 1024) return '${bytes.toInt()} B';
    if (bytes < 1024 * 1024) return '${(bytes / 1024).toStringAsFixed(1)} KB';
    return '${(bytes / (1024 * 1024)).toStringAsFixed(1)} MB';
  }
}
