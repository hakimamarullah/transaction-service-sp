import requests
import uuid
import random
from datetime import datetime, timedelta

API_URL = "http://localhost:8080/api/v1/transactions"
BEARER_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJlYmFua2luZy5jb20iLCJleHAiOjE3NTU1OTE0NzIsInVzZXJfaWQiOiJQLTAxMjM0NTY3ODkiLCJpYXQiOjE3NTU1OTA4NzIsInJvbGVzIjoiVVNFUiJ9.bTHGyTHUCezWBFdTMK4T_tA0cUfhHFuLIuP-G6_vqylkLQTz-OM4sVgcmlHv_OSTYES4l-wEP5dFlz9jElhtj9jUINJi1Bn9bIXTnlx2lrNvnK7aT1GLliCxqisEz40jgZ843h-5_E8HKi81Mwd85ODEZ9ppOIjLHuMsWbE-PHVf9hOJVimyumzzUslo5VGUhbKYIL72H9Mx2a_-Igw7H6cVZkrdI7e1js4KERHVJX8VfDuy_4DCHT4UxgpcQJX8vi7BmT6pGv6RCJ114X4aViB1DLYzck4rqk_yiU-Tog1Thx3NP-xIq6d_DXPpBAl3NvTq2lIdAP26Zf6w5fy8ng"
COUNT = 500

currencies = ["USD", "EUR", "CHF", "JPY", "IDR"]
types = ["CREDIT", "DEBIT"]
descriptions = [
    "Online payment",
    "ATM withdrawal",
    "Salary payment",
    "Shopping mall",
    "Transfer to friend",
    "Utility bill",
]

start_date = datetime(2020, 1, 1)
end_date = datetime(2023, 12, 31)

headers = {
    "Authorization": f"Bearer {BEARER_TOKEN}",
    "Content-Type": "application/json"
}

def random_date(start, end):
    delta = end - start
    return start + timedelta(days=random.randint(0, delta.days))

for i in range(COUNT):
    txn = {
        "id": str(uuid.uuid4()),
        "amount": round(random.uniform(10, 5000), 2),
        "currency": random.choice(currencies),
        "accountIban": f"CH92-0000-{random.randint(1000,9999)}-{random.randint(1000,9999)}-{random.randint(1000,9999)}",
        "valueDate": random_date(start_date, end_date).strftime("%Y-%m-%d"),
        "description": random.choice(descriptions),
        "customerId": "P-0123456789",
        "type": random.choice(types),
    }

    print(f"Creating transaction {i+1}... {txn['valueDate']}")
    response = requests.post(API_URL, json=txn, headers=headers)
    if response.status_code != 200:
        print(f"❌ Failed at {i}: {response.status_code} - {response.text}")
    else:
        print(f"✅ Transaction {i+1} created")

print(f"Done generating {COUNT} transactions 🚀")
