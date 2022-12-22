import os
import json

MEASUREMENT_PATH = os.path.expanduser('~/Downloads/measurement_1671720673785.json')


def main():
    measurement = json.load(open(MEASUREMENT_PATH))
    record_set = measurement['recordSet']
    cellular_key = list(filter(lambda x: 'CellularInfo' in x, record_set.keys()))[0]
    location_key = list(filter(lambda x: 'LocationInfo' in x, record_set.keys()))[0]
    subscription_key = list(filter(lambda x: 'SubscriptionInfo' in x, record_set.keys()))[0]
    cellular_data = record_set[cellular_key]['records']
    location_data = record_set[location_key]['records']
    # for i in range(10):
    #     print(cellular_data[i])
    for i in range(len(location_data)):
        print(location_data[i])


if __name__ == '__main__':
    main()
