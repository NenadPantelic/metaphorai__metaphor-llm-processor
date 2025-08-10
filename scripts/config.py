from dotenv import dotenv_values

config = dotenv_values(".env")

DB_USERNAME = "metaphorai-db-user"
DB_PASSWORD = "metaphorai-db-password"
DB_HOST = "localhost"
DB_PORT = 27017
DB_DATABASE = "metaphorai"
DB_COLLECTION = "documents"
DB_AUTHDB = "admin"


class MongoDBConnection:
    def __init__(
        self,
        username: str,
        password: str,
        host: str,
        port: int,
        database_name: str,
        collection_name: str,
        auth_source: str,
    ):
        self.username = username
        self.password = password
        self.host = host
        self.port = port
        self.database_name = database_name
        self.collection_name = collection_name
        self.auth_source = auth_source


MONGO_DB_CONNECTION = MongoDBConnection(
    config.get(DB_USERNAME),
    config.get(DB_PASSWORD),
    config.get(DB_HOST),
    config.get(DB_PORT, 27017),
    config.get(DB_DATABASE),
    config.get(DB_COLLECTION),
    config.get(DB_AUTHDB),
)
