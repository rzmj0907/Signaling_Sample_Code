import QtQuick 2.4
import QtQuick.Controls 2.5
import QtQuick.Controls.Material 2.3

Rectangle {
    id: callForm
    width: 330
    height: 150
    property alias reject: reject
    property alias accept: accept
    property alias cancel: cancel
    property alias callForm: callForm
    property alias call: call
    property alias account: account
    property alias btnLogout: btnLogout
    anchors.centerIn: parent
    border.width: 1
    border.color: "#9d9d9d"

    Button {
        id: btnLogout
        width: 100
        height: 30
        anchors.top: parent.top
        anchors.right: parent.right
        anchors.topMargin: 10
        anchors.rightMargin: 15
        text: qsTr("Logout")
    }

    Rectangle {
        anchors.fill: parent
        color: "#99000000"
        z: 10
        visible: callForm.state === "recvInvitation"

        Column {
            width: 260
            height: 180
            anchors.centerIn: parent
            spacing: 15

            Text {
                text: channelManager.fromAccount + " - 正在呼叫你..."
            }

            Button {
                id: accept
                width: parent.width
                text: qsTr("接受")
                highlighted: true
                Material.accent: Material.LightBlue
            }

            Button {
                id: reject
                width: parent.width
                text: qsTr("拒绝")
            }
        }
    }

    Rectangle {
        anchors.fill: parent
        color: "#99000000"
        z: 10
        visible: callForm.state === "calling"

        BusyIndicator {
            id: loading
            z: 11
            anchors.centerIn: parent
            running: callForm.state === "calling"
        }

        Button {
            id: cancel
            anchors.bottom: parent.bottom
            anchors.horizontalCenter: parent.horizontalCenter
            anchors.bottomMargin: 60
            text: qsTr("取消")
        }
    }

    Column {
        id: column
        width: 330
        height: 150
        anchors.verticalCenter: parent.verticalCenter
        anchors.horizontalCenter: parent.horizontalCenter
        spacing: 20

        TextField {
            id: account
            text: channelManager.callAccount
            width: parent.width
            placeholderText: qsTr("输入要邀请的账户")
            onTextChanged: channelManager.callAccount = text
        }

        Button {
            id: call
            width: parent.width
            text: qsTr("呼叫")
            highlighted: true
            Material.accent: Material.LightBlue
        }
    }
}
