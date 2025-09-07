from send_articles_to_rmq import execute
from time import sleep

from metaphor_db_handler import MetaphorDBHandler
from file_writer import IndexedDocumentFileWriter
from config import MONGO_DB_CONNECTION


NUM_OF_RUNS = 5
SLEEP_IN_SECONDS = 300

REPORTS_FOLDER = "reports"


if __name__ == "__main__":
    metaphor_db_handler = MetaphorDBHandler(MONGO_DB_CONNECTION)
    for i in range(1, NUM_OF_RUNS + 1):
        file_writer = IndexedDocumentFileWriter(f"{REPORTS_FOLDER}/{i}.txt")
        print(f"Executing iteration {i}")
        execute()

        print("Going to sleep to give some time to processor to complete the analysis")
        sleep(SLEEP_IN_SECONDS)

        docs = metaphor_db_handler.find_all_documents()
        for doc in docs:
            file_writer.write_document(doc)

        file_writer.close()
