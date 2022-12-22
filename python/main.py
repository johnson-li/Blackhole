import os
import json

MEASUREMENT_PATH = os.path.expanduser('~/Downloads/measurement_1671706614179.json')


def main():
    measurement = json.load(open(MEASUREMENT_PATH))
    print(measurement['createdAt'])
    record_set = measurement['recordSet']
    print(record_set.keys())


if __name__ == '__main__':
    main()
