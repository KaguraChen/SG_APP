import socket
import datetime
import cv2
import numpy as np
import threading
import time
import mediapipe as mp
from pyzbar import pyzbar

class Gesture_reg:
    def __init__(self):
        self.mpHands = mp.solutions.hands
        self.hands = self.mpHands.Hands(
            static_image_mode=False,
            max_num_hands=1,
            model_complexity=0,
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5
        )
        self.mpDraw = mp.solutions.drawing_utils
        self.t1 = time.time()
        self.px1 = [0] * 21
        self.py1 = [0] * 21

    def calc_v(self, t1, p1, t2, p2):
        return (p2 - p1) / (t2 - t1)

    def process(self, img):
        vx = 0
        vy = 0
        self.t2 = time.time()
        imgRGB = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        result = self.hands.process(imgRGB)
        if result.multi_hand_landmarks:
            for handLms in result.multi_hand_landmarks:
                self.mpDraw.draw_landmarks(img, handLms, self.mpHands.HAND_CONNECTIONS)
                for i, lm in enumerate(handLms.landmark):
                    if self.px1[-1] == 0 or self.py1[-1] == 0:
                        self.px1[i] = lm.x
                        self.py1[i] = lm.y
                    else:
                        vx += self.calc_v(self.t1, self.px1[i], self.t2, lm.x)
                        self.px1[i] = lm.x
                        vy += self.calc_v(self.t1, self.py1[i], self.t2, lm.y)
                        self.py1[i] = lm.y
        else:
            self.px1 = [0]*21
            self.py1 = [0] * 21
        if (abs(vx) > 40):
            print("向左") if vx < 0 else print("向右")
        elif (abs(vy) > 40):
            print("向上") if vy < 0 else print("向下")
        # print(v)
        self.t1 = self.t2
        return img

class ImgSocket:
    def __init__(self):
        self.HOST = socket.gethostbyname(socket.gethostname())
        self.PORT = 8080
        self.img = None
        self.cnt = 0
        self.mode_cnt = 0
        self.start = 0
        self.end = 0
        self.discnt_flag = False
        self.FPS = 0
        self.gesture = Gesture_reg()
        self.tcpSerSock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.tcpSerSock.bind((self.HOST, self.PORT))

    def scan_qrcode(self, qrcode):
        try:
            data = pyzbar.decode(qrcode)
            if data == []:
                return ""
            return data[0].data.decode('utf-8')
        except:
            return ""

    def recvall(self, sock, count):
        start = time.time()
        buf = b''  # buf是一个byte类型
        while count:
            end = time.time()
            if end - start > 1:
                raise TimeoutError
            # 接受TCP套接字的数据。数据以字符串形式返回，count指定要接收的最大数据量.
            newbuf = sock.recv(count)
            buf += newbuf
            count -= len(newbuf)
        return buf

    def add_text(self, img, msg):
        cv2.putText(img, f"FPS:{self.FPS}", (int(img.shape[1]*0.1), int(img.shape[0]*0.1)), cv2.FONT_HERSHEY_SIMPLEX,
                    0.5, (255, 255, 255))
        if msg != "":
            cv2.putText(img, msg, (int(img.shape[1]*0.3), int(img.shape[0]*0.1)), cv2.FONT_HERSHEY_SIMPLEX,
                        0.4, (255, 255, 255))
        return img

    def img_process(self, img):
        img = cv2.flip(cv2.transpose(img), 1)  # 由于接收到的图片方向不对，故在此旋转
        img = self.gesture.process(img)
        msg = self.scan_qrcode(img)
        img = self.add_text(img, msg)
        return img

    def recv_img(self, tcpCliSock, addr):
        while True:
            try:
                self.cnt += 1
                self.mode_cnt += 1
                if self.mode_cnt == 1:
                    self.start = time.time()  # 用于计算帧率信息
                # print("\n第{}次接收".format(self.cnt))
                length = int(self.recvall(tcpCliSock, 16))  # 获得图片文件的长度,16代表获取长度
                # print("收到的图片长度为:{}".format(length))

                data = self.recvall(tcpCliSock, length)  # 根据获得的文件长度，获取图片文件
                np_arr = np.frombuffer(data, np.uint8)
                img = cv2.imdecode(np_arr, cv2.IMREAD_COLOR)  # 将数组解码成图像
                # print(img.shape)
                self.img = self.img_process(img)

                img_bytes = cv2.imencode('.jpg', self.img, [cv2.IMWRITE_JPEG_QUALITY, 95])[1].tobytes()
                tcpCliSock.send(("%-16d" % len(img_bytes)).encode("utf-8"))
                # print("发送的图片长度为{}".format("%-16d" % len(img_bytes)))
                tcpCliSock.send(img_bytes)
                if self.mode_cnt == 5:
                    try:
                        self.end = time.time()
                        seconds = self.end - self.start
                        self.FPS = int(self.mode_cnt / seconds)
                        # print("FPS为{}".format(self.FPS))
                    except:
                        pass
                    finally:
                        self.mode_cnt = 0
            # except (TimeoutError, ConnectionResetError):
            except:
                print(addr, "断开连接")
                self.discnt_flag = True
                self.mode_cnt = 0
                self.cnt = 0
                break


    def run(self):
        self.tcpSerSock.listen(1)
        print(f"端口在{self.HOST}:{self.PORT}处开放")
        while True:
            print('\n等待连接...')
            tcpCliSock, addr = self.tcpSerSock.accept()
            print('连接时间:', datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S"))
            print('...连接IP:', addr)
            t = threading.Thread(target=self.recv_img, args=(tcpCliSock,addr))
            t.setDaemon(True)
            t.start()
            self.discnt_flag = False
            while True:
                if self.discnt_flag:
                    break
                try:
                    cv2.imshow("recv1", self.img)
                except Exception:
                    pass
                cv2.waitKey(1)
            cv2.destroyAllWindows()
            tcpCliSock.close()

if __name__ == '__main__':
    imskt = ImgSocket()
    imskt.run()