export interface FaqItem {
  question: string
  answer: string
}

export const publicFaqs: FaqItem[] = [
  {
    question: 'What is Nova SMS?',
    answer:
      'Nova SMS is a bulk SMS platform for Kenya. Organizations and event organizers create an account, fund a prepaid wallet, and send SMS from the dashboard or through the REST API. Delivery reports, contacts, and sender IDs are included.',
  },
  {
    question: 'How does Nova SMS M-Pesa STK Push work?',
    answer:
      'STK Push funds your Nova SMS wallet. You enter an amount and an M-Pesa phone number. Safaricom prompts that phone for a PIN. When the payment succeeds, Nova SMS credits the organization wallet once. Partner apps can start the same flow through POST /api/v1/wallet/topup and must poll Nova SMS until walletCredited is true.',
  },
  {
    question: 'Can I integrate Nova SMS into my website or backend?',
    answer:
      'Yes. Call the Nova SMS REST API from your backend with an API key (X-API-Key). Typical uses are sending SMS, checking delivery status, reading wallet balance, and starting an M-Pesa STK top-up. Do not put the live key in browser JavaScript.',
  },
  {
    question: 'Does Nova SMS support C2B Paybill payments?',
    answer:
      'Yes, for wallet funding. Pay to the platform Paybill using your organization account number (the M-Pesa account reference shown in Settings, such as NOVAC727). The BillRefNumber identifies the organization. Nova SMS credits that wallet from the Safaricom C2B confirmation. Do not send an organizationId from a client to choose who gets credited.',
  },
  {
    question: 'Does Nova SMS provide payment webhooks to my server?',
    answer:
      'Safaricom sends STK and C2B callbacks to Nova SMS. Your application should treat Nova SMS as the source of truth: poll POST /api/v1/wallet/topup/{id}/check until the status is COMPLETED and walletCredited is true, or FAILED. SMS delivery status is read from GET /api/v1/sms/{id}/status.',
  },
  {
    question: 'Is Nova SMS available for businesses in Kenya?',
    answer:
      'Yes. Business accounts are for ongoing sending. Event accounts are for short use such as weddings and community notices and stay active for about one week. Both types top up in Kenyan shillings via M-Pesa.',
  },
  {
    question: 'How do I get API credentials?',
    answer:
      'Create a Nova SMS organization account, then create an API client in the dashboard (API clients). Super Admin can also issue clients for an organization. The live key (nova_live_…) is shown only once. Store it as an environment variable on your server.',
  },
  {
    question: 'How do I test the API?',
    answer:
      'Use a live API key against https://smsapi.novastack.co.ke/api/v1. Start with POST /api/v1/sms/send. OpenAPI is available at /swagger-ui.html on the API host. Super Admin users also have an in-app test console. There is no separate public sandbox key type in the current product.',
  },
  {
    question: 'How are payment callbacks handled?',
    answer:
      'Daraja STK callbacks and C2B confirmation payloads are processed by Nova SMS. Credits are applied once per M-Pesa receipt. If a callback is delayed, POST /api/v1/wallet/topup/{id}/check can query Safaricom and complete a pending STK. Duplicate receipts are not credited twice.',
  },
]
