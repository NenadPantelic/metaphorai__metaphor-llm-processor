from typing import List
from data import IndexedDocument, Metaphor


class IndexedDocumentFileWriter:
    DOC_SEPARATOR = "-----------------"
    METAPHOR_SEPARATOR = "###"

    def __init__(self, filepath: str):
        self._file = open(filepath, "r")

    def write_document(self, document: IndexedDocument):
        print("Writing a document...")
        self._file.write(self.DOC_SEPARATOR)
        self._file.write("path\n")
        self._file.write(document.path + "\n\n")
        self._file.write("metaphors\n")
        self._write_metaphors(document.metaphors)
        self._file.write(self.DOC_SEPARATOR)
        print("Document writing is complete")

    def _write_metaphors(self, metaphors: List[Metaphor]):
        for metaphor in metaphors:
            self._file.write(self.METAPHOR_SEPARATOR)
            self._file.write("- phrase\n")
            self._file.write(metaphor.phrase + "\n")
            self._file.write("- offset\n")
            self._file.write(metaphor.offset + "\n")
            self._file.write("- type\n")
            self._file.write(metaphor.type + "\n")
            self._file.write("- explanation\n")
            self._file.write(metaphor.explanation + "\n")
            self._file.write(self.METAPHOR_SEPARATOR)

    def close(self):
        if self._file:
            self._file.close()
