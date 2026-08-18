# PHP integration

Call Nova SMS from PHP on the server. Do not embed the API key in frontend JavaScript.

```php
<?php
$baseUrl = getenv('NOVA_SMS_API_URL');
$apiKey = getenv('NOVA_SMS_API_KEY');

$payload = json_encode([
    'recipient' => '254712345678',
    'message' => 'Payment received.',
]);

$ch = curl_init($baseUrl . '/api/v1/sms/send');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_HTTPHEADER => [
        'X-API-Key: ' . $apiKey,
        'Content-Type: application/json',
        'Accept: application/json',
        'Idempotency-Key: payment-123456',
    ],
    CURLOPT_POSTFIELDS => $payload,
]);

$body = curl_exec($ch);
$status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

$result = json_decode($body, true);

if ($status < 200 || $status >= 300 || empty($result['success'])) {
    throw new RuntimeException($result['message'] ?? ('Nova SMS HTTP ' . $status));
}

$messageId = $result['data']['id'];
$smsStatus = $result['data']['status'];
```
