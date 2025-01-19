import cv2
import subprocess
import numpy
import argparse
import ffmpeg
from PIL import Image
from skimage.metrics import structural_similarity as ssim


def extract_frames_to_memory(video_path, frame_size=(480, 480), samples=10):
    interval = float(ffmpeg.probe(video_path)['format']['duration']) / (samples + 2)
    timestamps = [i * interval for i in range(1, (samples + 1))]
    frames = []
    for i, timestamp in enumerate(timestamps):
        out, _ = (ffmpeg
                  .input(video_path, ss=timestamp)
                  .filter("scale", frame_size[0], frame_size[1])
                  .output("pipe:1", vframes=1, format='rawvideo', pix_fmt='rgb24')
                  .run(capture_stdout=True, capture_stderr=subprocess.DEVNULL)
                  )

        frame = numpy.frombuffer(out, numpy.uint8).reshape((frame_size[1], frame_size[0], 3))
        frames.append(frame)
        # debug
        # Image.fromarray(frame).save(f"frame_{i}.png")
    return frames


def compare_frames(frame1, frame2):
    frame1_gray = cv2.cvtColor(frame1, cv2.COLOR_BGR2GRAY)
    frame2_gray = cv2.cvtColor(frame2, cv2.COLOR_BGR2GRAY)
    score, diff = ssim(frame1_gray, frame2_gray, full=True)
    psnr_value = cv2.PSNR(frame1, frame2)
    return score, psnr_value


def main():
    parser = argparse.ArgumentParser(description="Compare two videos for visual similarity.")
    parser.add_argument('video1', help="Path to the first video.")
    parser.add_argument('video2', help="Path to the second video.")

    args = parser.parse_args()

    # Extract frames from both videos and store them in ram
    frames1 = extract_frames_to_memory(args.video1)
    frames2 = extract_frames_to_memory(args.video2)

    ssims = []
    psnrs = []

    # Compare each corresponding pair of frames from both videos
    for f1, f2 in zip(frames1, frames2):
        ssim, psnr = compare_frames(f1, f2)
        ssims.append(ssim)
        psnrs.append(psnr)

    avg_ssim = numpy.mean(ssims) if ssims else 0
    avg_psnr = numpy.mean(psnrs) if psnrs else 0

    print(f"Average SSIM: {avg_ssim} out of {len(ssims)} frames compared")
    print(f"Average PSNR: {avg_psnr} out of {len(psnrs)} frames compared")

    # Determine if the videos are visually identical or not
    if avg_ssim >= 0.95 and avg_psnr >= 40:
        exit(0)  # Exit with code 0 for identical
    else:
        exit(1)  # Exit with code 1 for not identical


if __name__ == '__main__':
    main()
