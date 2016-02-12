# -*- coding: utf-8 -*-
"""\
This module converts the enron mail files in json (given a directory where .json files exist) to a CSV file format which will be imported to
a PSQL datase format.
It generates two files for the Document and Metadata tables.
Note: some email data do not contain a date value. In this case, an artificial date, which is the
current date is added. If this inappropriate causing wrong information, comment that line and in
stead continue to the next email, using the :continue keyword in the exption block

"""
import sys
import json
import datetime
import csv
import glob
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


def generate_csv(dir_name, document_out, metadata_out):
    """\
    Walks through the given `dir_name` which contains json files (with extension .json) and generates the CSV file `document_out` and
    metadata_out files which are to be imported to the Document and Metadata tables.
    """
    writer_meta = UnicodeWriter(open(metadata_out, 'wb'), delimiter=',', quotechar='"', escapechar='\\', quoting = csv.QUOTE_ALL)
    writer_doc = UnicodeWriter(open(document_out, 'wb'), delimiter=',', quotechar='"', escapechar='\\', quoting = csv.QUOTE_ALL)
    for filename in glob.glob(dir_name+'/*.json'):
        print(filename + " done")
        for line in tuple(open(filename, 'r')):
            enron  = json.loads(line)
            id = enron["id"]
            # Body of the emial document and the date : store them as Document
            try:
             writer_doc.writerow((str(id), enron["body"],  enron["date"]))
            except Exception, e: # date is missed - shall we add artificial date or ignore everything??
                now = datetime.datetime.now()
                writer_doc.writerow((str(id), enron["body"], now.strftime("%Y-%m-%d %H:%M:%S")))

            # Single element meta
            writer_meta.writerow((str(id), "Subject", enron["subject"], "Text"))
            writer_meta.writerow((str(id), "Timezone", enron["timezone"], "Text"))


            # Nested meta - recipients
            for r in enron["recipients"]:
                writer_meta.writerow((str(id), "Recipients.name", r["name"], "Text"))
                writer_meta.writerow((str(id), "Recipients.email", r["email"], "Text"))
                writer_meta.writerow((str(id), "Recipients.order", str(r["order"]), "Number"))
                writer_meta.writerow((str(id), "Recipients.type", r["type"], "Text"))
                writer_meta.writerow((str(id), "Recipients.id", str(r["id"]), "Number"))

            # sender metadats
            s = enron["sender"]
            writer_meta.writerow((str(id), "sender.id", str(s["id"]), "Number"))
            writer_meta.writerow((str(id), "sender.email", s["email"], "Text"))
            writer_meta.writerow((str(id), "sender.name", s["name"], "Text"))

if __name__ == '__main__':
    generate_csv(sys.argv[1], sys.argv[2], sys.argv[3]))
