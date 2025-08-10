import requests
import json


RMQ_USERNAME = "metaphorai"
RMQ_PASSWORD = "M3T4Ph0841"
## Basic bWV0YXBob3JhaTpNM1Q0UGgwODQx when base64 encoded

RMQ_URL = "http://localhost:15672/api/exchanges/%2f/x.article/publish"

HEADERS = {
    "Authorization": "Basic bWV0YXBob3JhaTpNM1Q0UGgwODQx",
    "Content-Type": "application/json",
}

GUARDIAN = "GUARDIAN"
INDEPENDENT = "INDEPENDENT"


def send_request_to_rmq(url: str, origin: str):
    payload = {"source": url, "origin": origin}
    req_body = json.dumps(
        {
            "vhost": "/",
            "properties": {},
            "routing_key": "",
            "payload": str(payload),
            "headers": {},
            "payload_encoding": "string",
        }
    )

    print(f"Sending request: {req_body}")
    response = requests.post(RMQ_URL, data=req_body, headers=HEADERS)
    print(response.text, response.status_code)


def get_origin_by_url(url: str) -> str:
    if GUARDIAN.lower() in url:
        return GUARDIAN

    if INDEPENDENT.lower() in url:
        return INDEPENDENT

    return None


def execute():
    article_urls = []
    with open("test-articles.txt", "r") as input_file:
        article_urls = [
            line[:-1] for line in input_file.readlines()
        ]  # remove trailing \n

    for article_url in article_urls:
        print(f"Dealing with an article: {article_url}")
        origin = get_origin_by_url(article_url)
        if not origin:
            print(f"Invalid origin for url {article_url}")
            continue

        send_request_to_rmq(article_url, origin)


if __name__ == "__main__":
    execute()
