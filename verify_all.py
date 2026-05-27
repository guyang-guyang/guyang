import urllib.request, json

HOST = 'http://47.108.209.71'
results = []

tests = [
    ('GET', '/backend/api/app/share_config.php', None, 'share_config'),
    ('GET', '/backend/api/user/share_complete.php', None, 'share_complete (GET)'),
    ('POST', '/backend/api/user/share_complete.php', '{}', 'share_complete (POST)'),
    ('GET', '/backend/api/user/invite_stats.php', None, 'invite_stats'),
    ('GET', '/landing.html', None, 'landing page'),
    ('GET', '/backend/api/app/apps.php', None, 'apps list'),
    ('GET', '/backend/api/info/list.php', None, 'info list'),
    ('GET', '/backend/api/app/banners.php', None, 'banners'),
    ('POST', '/backend/api/user/register.php', 
     json.dumps({'username':'functest2','password':'test123456','qq':'','invite_code':'','device_model':'test','device_sdk':'30'}), 
     'register'),
    ('POST', '/backend/api/admin/login.php',
     json.dumps({'username':'admin','password':'admin123'}),
     'admin login'),
]

for method, path, body, label in tests:
    try:
        url = HOST + path
        data_bytes = body.encode() if body else None
        req = urllib.request.Request(url, data=data_bytes, method=method)
        req.add_header('Content-Type', 'application/json')
        resp = urllib.request.urlopen(req, timeout=8)
        content = resp.read().decode()
        status = resp.status

        if 'code' in content:
            j = json.loads(content)
            code = j.get('code', '?')
            msg = j.get('message', '')
            has_data = 'data' in j
            data_keys = ''
            if has_data and isinstance(j.get('data'), dict):
                data_keys = ' keys=' + str(list(j['data'].keys())[:5])
            result = '[OK {}] {} {} -> code={} data={}{} {}'.format(
                status, method, label, code, has_data, data_keys, msg[:30])
            results.append(result)
        elif 'html' in content.lower()[:100]:
            results.append('[OK {}] {} {} -> HTML ({} bytes)'.format(status, method, label, len(content)))
        else:
            results.append('[OK {}] {} {} -> raw {} bytes'.format(status, method, label, len(content)))
    except Exception as e:
        results.append('[ERR] {} {} -> {}'.format(method, label, str(e)[:80]))

print('=== FUNCTIONAL VERIFICATION ===')
print()
for r in results:
    print(r)

print()
print('=== SUMMARY ===')
errors = sum(1 for r in results if '[ERR]' in r)
oks = sum(1 for r in results if '[OK' in r)
print('Tests: {}, Passed: {}, Failed: {}'.format(len(results), oks, errors))