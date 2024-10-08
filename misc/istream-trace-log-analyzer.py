import matplotlib.pyplot as plt
from math import log


def parse_message(msg):
    data = {}
    if 'time ' not in msg:
        return None  # Cannot parse
    thread_split = msg.split(' time ', 1)
    thread_name = thread_split[0].strip()
    rest = 'time ' + thread_split[1]  # Add back 'time ' to rest
    data['thread_name'] = thread_name
    tokens = rest.split()
    i = 0
    while i < len(tokens):
        token = tokens[i]
        if token == 'time':
            data['time'] = int(tokens[i + 1])
            i += 2
        elif token == 'pos':
            data['pos'] = int(tokens[i + 1])
            i += 2
        elif token == 'read' or token == 'request':
            data['event_type'] = token
            data['block_number'] = int(tokens[i + 1])
            i += 2
        elif token == 'buffer':
            data['buffer_blocks'] = int(tokens[i + 1])
            i += 3  # skip 'blocks' too
        elif token == 'got':
            data['got'] = int(tokens[i + 1])
            i += 2
        elif token == 'size':
            data['size'] = int(tokens[i + 1])
            i += 2
        elif token in ('start', 'end', 'prefetched'):
            data['status'] = token
            i += 1
        else:
            i += 1
    return data


def format_size(size):
    units = [
        ("bytes", 0),
        ("kB", 0),
        ("MB", 1),
        ("GB", 2),
        ("TB", 2),
        ("PB", 2)
    ]

    if size < 0:
        raise ValueError("Negative size not allowed")
    if size == 0:
        return "0 bytes"
    if size == 1:
        return "1 byte"

    exponent = min(int(log(size, 1024)), len(units) - 1)
    quotient = float(size) / 1024 ** exponent
    unit, precision = units[exponent]
    return f"{quotient:.{precision}f} {unit}"


def main():
    import sys

    if len(sys.argv) != 2:
        print("Usage: python script.py logfile")
        sys.exit(1)

    filename = sys.argv[1]

    read_events = {}
    request_events = {}

    with open(filename, 'r') as f:
        for line in f:
            if 'ISTREAM:' not in line:
                continue
            # Extract the message after 'ISTREAM: '
            parts = line.strip().split('ISTREAM: ', 1)
            if len(parts) != 2:
                continue
            msg = parts[1]
            data = parse_message(msg)
            if data is None:
                continue
            event_type = data.get('event_type')
            block_number = data.get('block_number')
            if event_type == 'read':
                key = block_number
                if key not in read_events:
                    read_events[key] = {}
                if data.get('status') == 'start':
                    read_events[key]['start_time'] = data.get('time')
                    read_events[key]['pos'] = data.get('pos')
                    read_events[key]['buffer_blocks'] = data.get('buffer_blocks')
                elif data.get('status') == 'end':
                    read_events[key]['end_time'] = data.get('time')
            elif event_type == 'request':
                key = block_number
                if key not in request_events:
                    request_events[key] = {}
                if data.get('status') == 'start':
                    request_events[key]['start_time'] = data.get('time')
                    request_events[key]['pos'] = data.get('pos')
                elif data.get('status') == 'end':
                    request_events[key]['end_time'] = data.get('time')
                    request_events[key]['got'] = data.get('got')
                    request_events[key]['size'] = data.get('size')
                elif data.get('status') == 'prefetched':
                    request_events[key]['prefetched_time'] = data.get('time')
                    request_events[key]['pos'] = data.get('pos')

    # Now process the events
    # For read events
    read_positions = []
    read_time_deltas = []

    read_start_positions = []
    read_end_positions = []
    read_start_times = []
    read_end_times = []

    for event in read_events.values():
        if 'start_time' in event and 'end_time' in event:
            delta_time = (event['end_time'] - event['start_time']) / 1e9  # Convert to seconds
            read_positions.append(event['pos'])
            read_time_deltas.append(delta_time)

            read_start_positions.append(event['pos'])
            read_end_positions.append(
                event['pos'] + event['buffer_blocks'] * 512)  # Assuming block size is 512 bytes
            read_start_times.append(event['start_time'])
            read_end_times.append(event['end_time'])

    # For request events
    request_positions = []
    request_time_deltas = []

    for event in request_events.values():
        if 'start_time' in event and 'end_time' in event:
            delta_time = (event['end_time'] - event['start_time']) / 1e9  # Convert to seconds
            request_positions.append(event['pos'])
            request_time_deltas.append(delta_time)

    # Compute average speed
    if read_start_positions and read_end_positions:
        total_bytes_read = read_end_positions[-1] - read_start_positions[0]
        total_time_elapsed = (read_end_times[-1] - read_start_times[0]) / 1e9  # Convert to seconds
        average_speed = total_bytes_read / total_time_elapsed
        print(f"Average speed: {format_size(average_speed)} bytes/second")
    else:
        print("Insufficient data to compute average speed.")

    # Plotting
    plt.figure(figsize=(10, 6))
    plt.plot(read_positions, read_time_deltas, 'o-', label='Read Events')
    plt.plot(request_positions, request_time_deltas, 's-', label='Request Events')
    plt.xlabel('Byte Position')
    plt.ylabel('Time Delta (seconds)')
    plt.title('Time Delta between Start and End of Read and Request Events')
    plt.legend()
    plt.grid(True)
    plt.show()


if __name__ == "__main__":
    main()
