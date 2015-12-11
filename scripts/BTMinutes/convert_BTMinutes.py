"""\
This module converts Spiegel BTMinutes TSV dataset to an generic intermediate representation.


The original BTMinutes.TSV file should be in the following format:
    <URL>	<Date>	<Content>	<ID>

The module expects two arguments where the first one (args[1]) is the original
TSV input file and the second one (args[2]) is the CSV output file.
"""
import sys
import csv
import codecs
import cStringIO

class UTF8Recoder:
    """
    Iterator that reads an encoded stream and reencodes the input to UTF-8
    """
    def __init__(self, f, encoding):
        self.reader = codecs.getreader(encoding)(f)

    def __iter__(self):
        return self

    def next(self):
        return self.reader.next().encode("utf-8")

class UnicodeReader:
    """\
    A CSV reader which will iterate over lines in the CSV file "f",
    which is encoded in the given encoding.
    """
    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        f = UTF8Recoder(f, encoding)
        self.reader = csv.reader(f, dialect=dialect, **kwds)

    def next(self):
        row = self.reader.next()
        return [unicode(s, "utf-8") for s in row]

    def __iter__(self):
        return self

class UnicodeWriter:
    """\
    Unicode Writer based on this SO
    http://stackoverflow.com/questions/17245415/read-and-write-csv-files-including-unicode-with-python-2-7
    """
    def __init__(self, f, dialect=csv.excel, encoding="utf-8", **kwds):
        self.queue = cStringIO.StringIO()
        self.writer = csv.writer(self.queue, dialect=dialect, **kwds)
        self.stream = f
        self.encoder = codecs.getincrementalencoder(encoding)()
    def writerow(self, row):
        '''writerow(unicode) -> None
        This function takes a Unicode string and encodes it to the output.
        '''
        self.writer.writerow([s.encode("utf-8") for s in row])
        data = self.queue.getvalue()
        data = data.decode("utf-8")
        data = self.encoder.encode(data)
        self.stream.write(data)
        self.queue.truncate(0)

    def writerows(self, rows):
        for row in rows:
            self.writerow(row)

def generate_csv(filename, out):
    """\
     Walks through the given TSV `filename` and generates the CSV file `out`
      """
    csv.field_size_limit(sys.maxsize)
    writer = UnicodeWriter(open(out, 'wb'), delimiter=',', quotechar='"', escapechar='\\', quoting = csv.QUOTE_ALL)
    with open(filename, 'rb') as f:
        for row in UnicodeReader(f,  delimiter='\t', quotechar='"', escapechar='\\', quoting = csv.QUOTE_ALL):
            url, date, content, id = row
            meta = [
                ("URL", url, "Text"),
                ("id", id, "Number")
            ]
            writer.writerow((content, date) + sum(meta,()))

if __name__ == '__main__':
    generate_csv(sys.argv[1], sys.argv[2])

