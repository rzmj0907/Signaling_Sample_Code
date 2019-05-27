import QtQuick 2.4

CallForm {
    call.onClicked: {
        callForm.state = "calling"
        call.enabled = false
        account.enabled = false
        btnLogout.enabled = false
        channelManager.call()
    }

    cancel.onClicked: {
        callForm.state = ""
        call.enabled = true
        account.enabled = true
        btnLogout.enabled = true
        channelManager.cancelInvite()
        channelManager.leaveChannel()
        channelManager.closeChannel()
    }

    accept.onClicked: {
        channelManager.accept()
    }

    reject.onClicked: {
        channelManager.reject()
    }

    btnLogout.onClicked: {
        loginManager.doLogout()
        pageLoader.setSource(Qt.resolvedUrl("Login.qml"))
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
        target: channelManager
        onCalledSignal: {
            if (resCode !== 200) {
                callForm.state = ""
                call.enabled = true
                account.enabled = true
                btnLogout.enabled = true

                messageDialog.title = "提示"
                messageDialog.text = "呼叫对方失败，错误代码：" + resCode
                messageDialog.visible = true
            }
        }
        onChannelNotifySignal: {
            switch (eventType)
            {
            case Call.EventType.NIMSignalingEventTypeInvite:
                callForm.state = "recvInvitation"
                break
            case Call.EventType.NIMSignalingEventTypeReject:
                callForm.state = ""
                break
            case Call.EventType.NIMSignalingEventTypeReject:
                callForm.state = ""
                call.enabled = true
                account.enabled = true
                btnLogout.enabled = true

                messageDialog.title = "提示"
                messageDialog.text = "对方拒绝了您的邀请。"
                messageDialog.visible = true
                break
            case Call.EventType.NIMSignalingEventTypeAccept:
                mainWindow.joinAgoraRoom()
                break
            }
        }
        onAcceptSignal: {
            if (resCode === 200) {
                mainWindow.joinAgoraRoom()
            } else {
                console.log("接受邀请失败，错误代码:" + resCode)
            }
        }
        onRejectSignal: {
            if (resCode === 200) {
                callForm.state = ""
                call.enabled = true
                account.enabled = true
                btnLogout.enabled = true
            } else {
                console.log("拒绝邀请失败，错误代码:" + resCode)
            }
        }
    }
}
