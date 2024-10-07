import matplotlib.pyplot as plt
import pandas as pd
import re
import sys

from math import log

# Check if the filename is provided
if len(sys.argv) < 2:
    print("Usage: python script_name.py your_log_file.log")
    sys.exit(1)

filename = sys.argv[1]

# Initialize a list to store event data<
events_data = []

# Updated regular expression pattern
pattern = re.compile(
    r".* time (\d+) pos (\d+) block (\d+) (\w+)(?: (.+))?"
)

# Open and read the log file
with open(filename, 'r') as file:
    for line in file:
        match = pattern.search(line)
        if match:
            time_ns = int(match.group(1))
            pos = int(match.group(2))
            block = int(match.group(3))
            event = match.group(4)
            action = match.group(5) if match.group(5) else ''

            # Consider only 'push' and 'write' events
            if event not in ['push', 'write']:
                continue  # Ignore other events

            # Determine event type
            if action == 'done':
                event_type = 'end'
            elif 'size' in action or (event == 'write' and action == ''):
                event_type = 'start'
            else:
                continue  # Ignore other events

            events_data.append({
                'time_ns': time_ns,
                'pos': pos,
                'block': block,
                'event': event,
                'event_type': event_type
            })

# After the parsing loop, print the number of events parsed
print(f"Total events parsed: {len(events_data)}")

# Check if events_data is not empty
if not events_data:
    print("No events were parsed from the log file.")
    sys.exit(1)

# Create a DataFrame
data = pd.DataFrame(events_data)

# Convert time from nanoseconds to seconds
data['time_s'] = data['time_ns'] / 1e9

# Separate data by event type
push_events = data[data['event'] == 'push']
write_events = data[data['event'] == 'write']


# Function to compute time deltas
def compute_time_deltas(events_df, event_name):
    # Separate start and end events
    start_events = events_df[events_df['event_type'] == 'start']
    end_events = events_df[events_df['event_type'] == 'end']

    # Merge start and end events on 'block'
    merged = pd.merge(start_events, end_events, on='block', suffixes=('_start', '_end'))

    # Compute time deltas
    merged['time_delta_s'] = merged['time_s_end'] - merged['time_s_start']

    # Remove negative time deltas (if any)
    merged = merged[merged['time_delta_s'] >= 0]

    # Keep relevant columns
    result = merged[['block', 'pos_start', 'time_delta_s']]
    result['event'] = event_name

    return result


# Compute time deltas for 'push' and 'write'
push_deltas = compute_time_deltas(push_events, 'push')
write_deltas = compute_time_deltas(write_events, 'write')

# Combine the deltas for analysis
deltas = pd.concat([push_deltas, write_deltas], ignore_index=True)


# Identify outliers (e.g., using 1.5 * IQR rule)
def identify_outliers(df):
    outlier_indices = []
    for event_name, group in df.groupby('event'):
        # Q1 = group['time_delta_s'].quantile(0.25)
        # Q3 = group['time_delta_s'].quantile(0.75)
        # IQR = Q3 - Q1
        # threshold = 1.9 * IQR
        outliers = group[group['time_delta_s'] > 0.3]
        outlier_indices.extend(outliers.index.tolist())
    return outlier_indices


outlier_indices = identify_outliers(deltas)
outliers = deltas.loc[outlier_indices]

# Plot time deltas with outliers annotated
plt.figure(figsize=(12, 6))
for event_name, group in deltas.groupby('event'):
    plt.plot(group['block'], group['time_delta_s'], label=f'{event_name.capitalize()} Duration',
             marker='o')

# Annotate outliers
for idx, row in outliers.iterrows():
    plt.annotate(f"Block {int(row['block'])}", (row['block'], row['time_delta_s']),
                 textcoords="offset points", xytext=(0, 10), ha='center', color='red')

# Optional: Print statistics
stats = deltas.groupby('event')['time_delta_s'].agg(['mean', 'max', 'min', 'std'])
print("Event Duration Statistics:")
print(stats)


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


e0 = events_data[0]
e1 = events_data[-1]

t_delta_ns = e1["time_ns"] - e0["time_ns"]
t_delta_s = t_delta_ns / 1e9
blocks_delta = e1["block"] - e0["block"]
bytes_delta = blocks_delta * 512

print("Speed:", format_size(bytes_delta / t_delta_s), "/s")
print("Total time:", round(t_delta_s, 2), "seconds")

plt.xlabel('Block Number')
plt.ylabel('Duration (seconds)')
plt.title('Time Delta between Start and End of Events (Annotated Outliers)')
plt.legend()
plt.grid(True)
plt.show()