import socket
import pyaudio
import numpy as np
import matplotlib.pyplot as plt
from matplotlib import rcParams
import threading
from matplotlib.animation import FuncAnimation

# 设置字体
rcParams['font.sans-serif'] = ['SimHei']  # 黑体
rcParams['axes.unicode_minus'] = False   # 解决负号显示问题


# 音频配置
CHUNK = 4096
FORMAT = pyaudio.paInt16
CHANNELS = 1
RATE = 44100

# 初始化音频播放
p = pyaudio.PyAudio()
stream = p.open(format=FORMAT,
                channels=CHANNELS,
                rate=RATE,
                output=True)

# 服务器配置
HOST = '0.0.0.0'
PORT = 7000
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.bind((HOST, PORT))
server_socket.listen(1)

print("等待客户端连接...")
conn, addr = server_socket.accept()
print(f"已连接：{addr}")

# 音频数据缓冲区
audio_buffer = []
buffer_lock = threading.Lock()

# 音频接收和播放线程
def audio_receive_and_play():
    global audio_buffer
    while True:
        try:
            data = conn.recv(CHUNK)
            if not data:
                break
            stream.write(data)  # 播放音频
            with buffer_lock:
                audio_buffer.append(data)  # 将数据存入缓冲区
                if len(audio_buffer) > 50:  # 限制缓冲区大小
                    audio_buffer.pop(0)
        except Exception as e:
            print(f"音频接收错误: {e}")
            break

# 可视化线程
def update_loudness(frame):
    global audio_buffer
    with buffer_lock:
        if not audio_buffer:
            return line,
        data = audio_buffer[-1]  # 取最新的数据
        audio_data = np.frombuffer(data, dtype=np.int16)
        rms = np.sqrt(np.mean(audio_data ** 2))  # 计算响度
        y[:-1] = y[1:]  # 左移数据
        y[-1] = rms
        line.set_ydata(y)
    return line,

# 初始化可视化
fig, ax = plt.subplots()
x = np.arange(0, 50)
y = np.zeros(50)
line, = ax.plot(x, y, label="响度")
ax.set_title("实时响度图")
ax.set_xlabel("帧序号")
ax.set_ylabel("响度 (RMS)")
ax.set_ylim(0, 150)
ax.legend(loc="upper right")

# 创建并启动音频接收线程
audio_thread = threading.Thread(target=audio_receive_and_play, daemon=True)
audio_thread.start()

# 启动可视化
ani = FuncAnimation(fig, update_loudness, interval=50)

try:
    plt.show()
except KeyboardInterrupt:
    print("可视化结束")

# 关闭资源
conn.close()
stream.stop_stream()
stream.close()
p.terminate()
server_socket.close()
