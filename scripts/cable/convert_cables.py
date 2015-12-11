# -*- coding: utf-8 -*-
"""\
This module converts the original cable.csv to an generic intermediate representation.
The script also adds additional meta data that is not present in the original cable.csv:

The original cables.csv should be in the following format:
    <identifier>, <creation-date>, <reference-id>, <origin>, <classification-level>, 
    <references-to-other-cables>, <header>, <body>

The module expects two arguments where the first one (args[1]) is the original
CSV input file and the second one (args[2]) is the CSV output file.
"""
import sys
import csv
import codecs
import cStringIO
from cablemap.core import cables_from_source
from cablemap.core.utils import titlefy, cables_from_csv

# Source: <http://docs.python.org/library/csv.html>
class UnicodeWriter:
    """
    A CSV writer which will write rows to CSV file "f",
    which is encoded in the given encoding.
    """

    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        # Redirect output to a queue
        self.queue = cStringIO.StringIO()
        self.writer = csv.writer(self.queue, dialect=dialect, **kwds)
        self.stream = f
        self.encoder = codecs.getincrementalencoder(encoding)()

    def writerow(self, row):
        self.writer.writerow([s.encode("utf-8") for s in row])
        # Fetch UTF-8 output from the queue ...
        data = self.queue.getvalue()
        data = data.decode("utf-8")
        # ... and reencode it into the target encoding
        data = self.encoder.encode(data)
        # write to the target stream
        self.stream.write(data)
        # empty queue
        self.queue.truncate(0)

    def writerows(self, rows):
        for row in rows:
            self.writerow(row)


def generate_csv(filename, out):
    """\
    Walks through the given csv `filename` and generates the CSV file `out`
    """
    writer = UnicodeWriter(open(out, 'wb'), delimiter=',', quotechar='"', escapechar='\\', quoting = csv.QUOTE_ALL)
    for cable in cables_from_csv(filename):    

        # Single element meta
        single = [
            ("ReferenceId", cable.reference_id, "Text"),
            ("Origin", cable.origin, "Text"),
            ("Classification", cable.classification, "Text"),
            ("Subject", titlefy(cable.subject), "Text"),
            ("Header", cable.header, "Text")
        ]

        # Multi element meta
        tags = [("Tags", x, "Text") for x in cable.tags]
        recipients = [("Recipients", x.name, "Text") for x in cable.recipients]
        references = [("References", x.value, "Text") for x in cable.references]
        singed_by = [("SignedBy", x, "Text") for x in cable.signed_by]
        
        meta = sum(single + tags + recipients + references + singed_by, ())
        writer.writerow((cable.content, cable.created) + meta)


if __name__ == '__main__':
    generate_csv(sys.argv[1], sys.argv[2])
