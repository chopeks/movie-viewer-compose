import subprocess
import json
import sys
import numpy as np
import concurrent.futures
from pymediainfo import MediaInfo


def get_duration(file_path):
    try:
        media_info = MediaInfo.parse(file_path)
        for track in media_info.tracks:
            if track.track_type == "Video" and track.duration:
                return float(track.duration) / 1000
    except Exception:
        pass

    ffprobe_cmd = [
        "ffprobe",
        "-v", "error",
        "-select_streams", "v:0",
        "-show_entries", "stream=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        file_path
    ]
    try:
        result = subprocess.run(ffprobe_cmd, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True)
        if result.stdout.strip():
            return float(result.stdout.strip())
    except Exception:
        pass

    return None


def get_raw_fingerprint(file_path, start=None, duration=None):
    ffmpeg_cmd = ['ffmpeg']
    if start is not None:
        ffmpeg_cmd += ['-ss', str(start)]

    ffmpeg_cmd += ['-i', file_path]
    if duration is not None:
        ffmpeg_cmd += ['-t', str(duration)]

    ffmpeg_cmd += [
        '-vn',
        '-ac', '1',
        '-ar', '16000',
        '-f', 's16le',
        '-'
    ]

    fpcalc_cmd = [
        'fpcalc',
        '-format', 's16le',
        '-rate', '16000',
        '-json',
        '-raw',
        '-length', '0',
        '-'
    ]

    p1 = subprocess.Popen(ffmpeg_cmd, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    p2 = subprocess.Popen(fpcalc_cmd, stdin=p1.stdout, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)

    stdout, _ = p2.communicate()

    if not stdout:
        return None

    data = json.loads(stdout)
    return np.array(data.get('fingerprint'), dtype=np.uint32)


def get_fragment(dur, len):
    sample_len = min(dur, len)
    middle = dur / 2
    return middle - sample_len / 2, sample_len


def find_fragment(movieA, movieB):
    t_start = time.perf_counter()
    durA = get_duration(movieA)
    durB = get_duration(movieB)

    # determine shorter and longer movie
    if durA <= durB:
        shorter = movieA
        longer = movieB
        shorter_dur = durA
        longer_dur = durB
    else:
        shorter = movieB
        longer = movieA
        shorter_dur = durB
        longer_dur = durA


    with concurrent.futures.ThreadPoolExecutor() as executor:
        start, duration = get_fragment(shorter_dur, 120)
        future_needle = executor.submit(get_raw_fingerprint, shorter, start=max(0, start), duration=duration)
        start, duration = get_fragment(longer_dur, longer_dur - shorter_dur + 180)
        future_haystack = executor.submit(get_raw_fingerprint, longer, start=max(0, start), duration=duration)

        needle = future_needle.result()
        haystack = future_haystack.result()

    if needle is None or haystack is None:
        print("Failed to generate fingerprints.")
        return

    n_len, h_len = len(needle), len(haystack)

    best_dist = float('inf')

    sample_step = 3
    sparse_needle = needle[::sample_step]

    for i in range(h_len - n_len + 1):
        hay_slice = haystack[i:i + n_len:sample_step]
        diff = np.bitwise_xor(sparse_needle, hay_slice)
        dist = sum(int.bit_count(int(x)) for x in diff)
        if dist < best_dist:
            best_dist = dist

    max_bits = 32 * len(sparse_needle)
    confidence = 1.0 - (best_dist / max_bits)

    result = {
        "confidence": round(confidence, 4),
        "elapsed": round((time.perf_counter() - t_start) * 1000) / 1000
    }

    print("RESULT_JSON:" + json.dumps(result))


import time

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python audio.py <movie> <fragment>")
    else:


        find_fragment(sys.argv[1], sys.argv[2])

