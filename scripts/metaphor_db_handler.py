import pymongo
import urllib.parse
from typing import List

from config import MongoDBConnection, MONGO_DB_CONNECTION
from data import IndexedDocument, Metaphor


class MetaphorDBHandler:
    def __init__(self, mongodb_connection: MongoDBConnection):
        encoded_password = urllib.parse.quote_plus(mongodb_connection.password)
        mongo_uri = f"mongodb://{mongodb_connection.username}:{encoded_password}@{mongodb_connection.host}:{mongodb_connection.port}/{mongodb_connection.database_name}?authSource={mongodb_connection.auth_source}"
        try:
            client = pymongo.MongoClient(mongo_uri)
            db = client[mongodb_connection.database_name]
            self._collection = db[mongodb_connection.collection_name]
            print("Successfully connected to MongoDB with authentication!")
        except pymongo.errors.ConnectionFailure as e:
            print(f"Could not connect to MongoDB: {e}")
            raise e
        except pymongo.errors.OperationFailure as e:
            print(f"Authentication failed: {e}")
            raise e

    def find_all_documents(self):
        print("Retrieving all documents...")
        cursor = self._collection.find({})
        return [self._convert_to_document(document) for document in cursor]

    def clear_all(self):
        self._collection.delete_many({})
        print("All documents are deleted")

    def _convert_to_document(self, document_dict: dict) -> IndexedDocument:
        return IndexedDocument(
            document_dict.get("name"),
            document_dict.get("path"),
            document_dict.get("text"),
            self._convert_to_metaphors(document_dict.get("metaphors")),
        )

    def _convert_to_metaphors(self, metaphor_dicts: dict) -> List[Metaphor]:
        metaphors = []
        for metaphor_dict in metaphor_dicts:
            metaphor = Metaphor(
                metaphor_dict.get("phrase"),
                metaphor_dict.get("offset"),
                metaphor_dict.get("type"),
                metaphor_dict.get("explanation"),
            )
            metaphors.append(metaphor)

        return metaphors
