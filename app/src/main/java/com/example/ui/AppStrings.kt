package com.example.ui

object AppStrings {
    fun get(key: String, isBengali: Boolean): String {
        return if (isBengali) bnMap[key] ?: enMap[key] ?: key else enMap[key] ?: key
    }

    private val enMap = mapOf(
        "app_title" to "SMS READER X",
        "tagline" to "Authorized Merchant SMS Payment Reconciliation",
        "tab_dashboard" to "Dashboard",
        "tab_inbox" to "Inbox",
        "tab_settings" to "API Settings",
        "tab_senders" to "Senders & Parser",
        "tab_history" to "History",
        "tab_privacy" to "Privacy & Policy",

        // Dashboard
        "today_transactions" to "Today's Volume",
        "successful_submissions" to "Successful Submissions",
        "pending_review" to "Pending Review",
        "failed_submissions" to "Failed Submissions",
        "monitoring_active" to "Monitoring Active",
        "monitoring_stopped" to "Monitoring Stopped",
        "start_monitoring" to "Start Monitoring",
        "stop_monitoring" to "Stop Monitoring",
        "quick_test_banner" to "Quick Sample SMS Test",
        "quick_test_desc" to "Simulate an approved payment notification (bKash/Nagad/Rocket/Upay) to test parser & review flow.",
        "test_sample_button" to "Simulate Payment SMS",
        "last_sync" to "Last Sync",
        "connection_status" to "HTTPS Endpoint Status",

        // Inbox
        "search_placeholder" to "Search by Txn ID, Provider, or Ref...",
        "filter_all" to "All",
        "filter_pending" to "Pending",
        "filter_sent" to "Sent",
        "filter_failed" to "Failed",
        "filter_duplicate" to "Duplicate",
        "empty_inbox_title" to "No Transactions Yet",
        "empty_inbox_desc" to "Incoming approved payment SMS will appear here for manual merchant review.",

        // Details
        "transaction_details" to "Transaction Details",
        "provider" to "Provider",
        "transaction_id" to "Transaction ID",
        "amount" to "Amount",
        "currency" to "Currency",
        "sender_id" to "Sender ID",
        "reference" to "Customer Reference",
        "balance_detected" to "Parsed Balance",
        "parsing_confidence" to "Parsing Confidence",
        "original_sms" to "Original SMS Preview (Sanitized)",
        "send_to_gateway" to "Send to Gateway",
        "mark_reviewed" to "Mark Reviewed",
        "ignore_txn" to "Ignore",
        "delete_record" to "Delete Record",

        // API Settings
        "api_settings_title" to "Reconciliation API Settings",
        "api_url_label" to "HTTPS Gateway URL (POST only)",
        "api_url_helper" to "Must begin with https://. HTTP endpoints are strictly prohibited.",
        "auth_type_label" to "Authentication Scheme",
        "auth_header_label" to "Header Name (e.g. Authorization or X-API-KEY)",
        "secret_value_label" to "Secret Value / Bearer Token",
        "secret_encrypted_badge" to "Protected by Android Keystore (AES-256 GCM)",
        "merchant_id_label" to "Merchant ID",
        "device_id_label" to "Terminal / Device ID",
        "timeout_label" to "Request Timeout (Seconds)",
        "prevent_duplicates_label" to "Strict Duplicate Prevention",
        "test_api_button" to "Test API with Safe Mock Payload",
        "save_settings_button" to "Save API Settings",
        "endpoint_domain" to "Target Domain",

        // Senders & Parser
        "approved_senders_title" to "Approved Sender IDs",
        "add_sender" to "Add Approved Sender",
        "sender_hint" to "e.g. bKash, NAGAD, 16216, Upay",
        "parser_tester_title" to "Live SMS Parser Sandbox",
        "parser_tester_desc" to "Test how custom SMS messages are parsed before live monitoring.",
        "test_parse_action" to "Parse & Evaluate",

        // Preview Dialog
        "preview_title" to "API Request Payload Preview",
        "confirm_send" to "Confirm and Send",
        "cancel" to "Cancel",
        "preview_warning" to "Please verify that the transaction amount and reference match your physical sale before transmitting.",

        // Privacy Policy
        "privacy_title" to "Privacy Policy & Google Play Compliance",
        "data_deletion_title" to "Merchant Data & Record Deletion",
        "wipe_all_data" to "Wipe All Local Records & Reset App",
        "export_csv" to "Export CSV Report",
        "audit_logs_title" to "Local Action Audit Log"
    )

    private val bnMap = mapOf(
        "app_title" to "SMS READER X",
        "tagline" to "অনুমোদিত মার্চেন্ট এসএমএস পেমেন্ট রিকনসিলিয়েশন",
        "tab_dashboard" to "ড্যাশবোর্ড",
        "tab_inbox" to "ইনবক্স",
        "tab_settings" to "এপিআই সেটিংস",
        "tab_senders" to "সেন্ডার ও পার্সার",
        "tab_history" to "ইতিহাস",
        "tab_privacy" to "গোপনীয়তা নীতি",

        // Dashboard
        "today_transactions" to "আজকের মোট লেনদেন",
        "successful_submissions" to "সফল সাবমিশন",
        "pending_review" to "রিভিউ বাকি",
        "failed_submissions" to "ব্যর্থ সাবমিশন",
        "monitoring_active" to "মনিটরিং চালু আছে",
        "monitoring_stopped" to "মনিটরিং বন্ধ",
        "start_monitoring" to "মনিটরিং চালু করুন",
        "stop_monitoring" to "মনিটরিং বন্ধ করুন",
        "quick_test_banner" to "নমুনা পেমেন্ট এসএমএস পরীক্ষা",
        "quick_test_desc" to "বিকাশ, নগদ, রকেট বা উপায়ের নমুনা এসএমএস দিয়ে পার্সার পরীক্ষা করুন।",
        "test_sample_button" to "নমুনা এসএমএস টেস্ট",
        "last_sync" to "সর্বশেষ সিঙ্ক",
        "connection_status" to "HTTPS গেটওয়ে অবস্থা",

        // Inbox
        "search_placeholder" to "লেনদেন আইডি, প্রদানকারী বা রেফারেন্স খুঁজুন...",
        "filter_all" to "সবগুলো",
        "filter_pending" to "অপেক্ষমান",
        "filter_sent" to "প্রেরিত",
        "filter_failed" to "ব্যর্থ",
        "filter_duplicate" to "ডুপ্লিকেট",
        "empty_inbox_title" to "কোন লেনদেন পাওয়া যায়নি",
        "empty_inbox_desc" to "অনুমোদিত সেন্ডার থেকে পেমেন্ট এসএমএস আসলে এখানে জমা হবে।",

        // Details
        "transaction_details" to "লেনদেনের বিস্তারিত",
        "provider" to "প্রদানকারী",
        "transaction_id" to "লেনদেন আইডি (TrxID)",
        "amount" to "টাকার পরিমাণ",
        "currency" to "মুদ্রা",
        "sender_id" to "সেন্ডার আইডি",
        "reference" to "গ্রাহক রেফারেন্স",
        "balance_detected" to "শনাক্তকৃত ব্যালেন্স",
        "parsing_confidence" to "পার্সিং নির্ভুলতা স্কোর",
        "original_sms" to "মূল এসএমএস (সংরক্ষিত ও সুরক্ষিত)",
        "send_to_gateway" to "গেটওয়েতে পাঠান",
        "mark_reviewed" to "পর্যালোচিত চিহ্নিত করুন",
        "ignore_txn" to "উপেক্ষা করুন",
        "delete_record" to "রেকর্ড মুছুন",

        // API Settings
        "api_settings_title" to "রিকনসিলিয়েশন এপিআই কনফিগারেশন",
        "api_url_label" to "HTTPS গেটওয়ে ইউআরএল (শুধুমাত্র POST)",
        "api_url_helper" to "অবশ্যই https:// দিয়ে শুরু হতে হবে। HTTP সম্পূর্ণ নিষিদ্ধ।",
        "auth_type_label" to "অথেনটিকেশন স্কিম",
        "auth_header_label" to "হেডার নেম (যেমন Authorization বা X-API-KEY)",
        "secret_value_label" to "সিক্রেট কি / বিয়ারার টোকেন",
        "secret_encrypted_badge" to "অ্যান্ড্রয়েড কী-স্টোর দ্বারা এনক্রিপ্টকৃত (AES-256 GCM)",
        "merchant_id_label" to "মার্চেন্ট আইডি",
        "device_id_label" to "টার্মিনাল / ডিভাইস আইডি",
        "timeout_label" to "টাইমআউট (সেকেন্ড)",
        "prevent_duplicates_label" to "ডুপ্লিকেট লেনদেন প্রতিরোধ",
        "test_api_button" to "নমুনা ডেটা দিয়ে এপিআই টেস্ট",
        "save_settings_button" to "সেটিংস সংরক্ষণ করুন",
        "endpoint_domain" to "টার্গেট ডোমেইন",

        // Senders & Parser
        "approved_senders_title" to "অনুমোদিত সেন্ডার তালিকা",
        "add_sender" to "নতুন সেন্ডার যুক্ত করুন",
        "sender_hint" to "যেমন: bKash, NAGAD, 16216, Upay",
        "parser_tester_title" to "লাইভ এসএমএস পার্সার স্যান্ডবক্স",
        "parser_tester_desc" to "যেকোনো বাংলা বা ইংরেজি এসএমএস পার্স করে দেখুন।",
        "test_parse_action" to "পার্স এবং যাচাই",

        // Preview Dialog
        "preview_title" to "এপিআই রিকোয়েস্ট পেলোড প্রিভিউ",
        "confirm_send" to "যাচাই ও প্রেরণ করুন",
        "cancel" to "বাতিল",
        "preview_warning" to "পাঠানোর পূর্বে নিশ্চিত করুন যে টাকার পরিমাণ ও রেফারেন্স আপনার মূল বিক্রির সাথে মেলে।",

        // Privacy Policy
        "privacy_title" to "গোপনীয়তা নীতি ও গুগল প্লে নীতি",
        "data_deletion_title" to "মার্চেন্ট ডেটা ও রেকর্ড মুছে ফেলা",
        "wipe_all_data" to "সকল লোকাল ডেটা মুছুন ও রিসেট করুন",
        "export_csv" to "সিএসভি রিপোর্ট ডাউনলোড",
        "audit_logs_title" to "লোকাল অডিট লগ"
    )
}
