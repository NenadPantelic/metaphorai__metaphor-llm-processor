from typing import List


class Metaphor:
    def __init__(self, phrase: str, offset: int, _type: str, explanation: str):
        self.phrase = phrase
        self.offset = offset
        self.type = _type
        self.explanation = explanation


class IndexedDocument:
    def __init__(self, path: str, metaphors: List[Metaphor]):
        self.path = path
        self.metaphors = metaphors
