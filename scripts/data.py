from typing import List


class Metaphor:
    def __init__(self, phrase: str, offset: int, _type: str, explanation: str):
        self.phrase = phrase
        self.offset = offset
        self.type = _type
        self.explanation = explanation


class IndexedDocument:
    def __init__(self, name:str, path: str,text:str, metaphors: List[Metaphor]):
        self.name = name
        self.path = path
        self.text = text
        self.metaphors = metaphors
        
