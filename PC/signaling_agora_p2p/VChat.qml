import QtQuick 2.4

VChatForm {
    microphone.onClicked: {
        if (microphone.state === "disabled") {
            agoraRtcEngine.muteLocalAudioStream(true)
            microphone.state = ""
        } else {
            agoraRtcEngine.muteLocalAudioStream(false)
            microphone.state = "disabled"
        }
    }

    hangup.onClicked: {
        agoraRtcEngine.leaveChannel()
        channelManager.leaveChannel()
    }

    camera.onClicked: {
        if (camera.state === "disabled") {
            agoraRtcEngine.enableLocalVideo(true)
            camera.state = ""
        } else {
            agoraRtcEngine.enableLocalVideo(false)
            camera.state = "disabled"
        }
    }

    Component.onCompleted: {
        agoraRtcEngine.setupLocalVideo(local);
    }

    enum EventType {
        NIMSignalingEventTypeClose			= 1, /**< 返回NIMSignalingNotityInfoClose，支持在线、离线通知 */
        NIMSignalingEventTypeJoin			= 2, /**< 返回NIMSignalingNotityInfoJoin，支持在线、离线通知 */
        NIMSignalingEventTypeInvite         = 3, /**< 返回NIMSignalingNotityInfoInvite，支持在线、离线通知 */
        NIMSignalingEventTypeCancelInvite	= 4, /**< 返回NIMSignalingNotityInfoCancelInvite，支持在线、离线通知 */
        NIMSignalingEventTypeReject         = 5, /**< 返回NIMSignalingNotityInfoReject，支持在线、多端同步、离线通知 */
        NIMSignalingEventTypeAccept         = 6, /**< 返回NIMSignalingNotityInfoAccept，支持在线、多端同步、离线通知 */
        NIMSignalingEventTypeLeave			= 7, /**< 返回NIMSignalingNotityInfoLeave，支持在线、离线通知 */
        NIMSignalingEventTypeCtrl			= 8
    }

    Connections {
        target: agoraRtcEngine
        onUserJoined: {
            console.log("On user joined, user ID:" + uid)
            agoraRtcEngine.setupRemoteVideo(uid, remote);
        }
        onFirstRemoteVideoDecoded: {
            console.log("On receive first remote video decoded, user ID:" + uid)
            chatroom.state = "connected"
        }
        onUserOffline: {
            console.log("On user offline, user ID:" + uid)
            agoraRtcEngine.leaveChannel()
        }
    }

    Connections {
        target: channelManager
        onChannelNotifySignal: {
            // 通话中拒绝所有邀请请求
            if (eventType === VChat.EventType.NIMSignalingEventTypeInvite) {
                channelManager.reject()
            }

            if (eventType === VChat.EventType.NIMSignalingEventTypeLeave) {
                channelManager.leaveChannel()
            }
        }

        onLeaveChannelSignal: {
            pageLoader.setSource(Qt.resolvedUrl("Call.qml"))
        }
    }
}
