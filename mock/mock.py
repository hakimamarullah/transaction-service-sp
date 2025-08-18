import requests
import uuid
import random
from datetime import datetime, timedelta

API_URL = "http://localhost:8080/api/v1/transactions"

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

def random_date(start, end):
    delta = end - start
    return start + timedelta(days=random.randint(0, delta.days))

for i in range(1000):
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
    response = requests.post(API_URL, json=txn)
    if response.status_code != 200:
        print(f"❌ Failed at {i}: {response.status_code} - {response.text}")
    else:
        print(f"✅ Transaction {i+1} created")

print("Done generating 1000 transactions 🚀")
