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

    inviteButton.onClicked: {
        channelManager.inviteMember()
        inviteButton.state = "inviting"
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
            console.log(eventType)
            // 通话中拒绝所有邀请请求
            if (eventType === VChat.EventType.NIMSignalingEventTypeInvite) {
                channelManager.reject()
            }

            // 被叫方接受邀请后状态修改为 connected 界面跟随改变
            if (eventType === VChat.EventType.NIMSignalingEventTypeAccept) {
                chatroom.state = "connected"
            }

            if (eventType === VChat.EventType.NIMSignalingEventTypeLeave) {
                channelManager.leaveChannel()
            }
        }

        onLeaveChannelSignal: {
            pageLoader.setSource(Qt.resolvedUrl("CreateChannel.qml"))
        }

        onInviteMemberSignal: {
            inviteButton.state = ""
            if (resCode == 200) {
                console.log("Invite successfully: " + resCode)
            } else {
                console.log("Failed to invite account，error code: " + resCode)
            }
        }
    }
}
