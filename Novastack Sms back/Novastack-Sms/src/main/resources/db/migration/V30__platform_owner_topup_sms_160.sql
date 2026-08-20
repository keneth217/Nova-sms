UPDATE platform_sms_settings
SET template_platform_topup = 'Nova SMS: {name} ({account}) topped up KES {amount}.{receipt} Bal KES {balance}. {time}'
WHERE id = 1
  AND template_platform_topup = 'Nova SMS: {name} ({account}) wallet credited KES {amount}.{receipt} Balance: KES {balance}.';
