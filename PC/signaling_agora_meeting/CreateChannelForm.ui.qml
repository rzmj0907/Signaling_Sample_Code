import QtQuick 2.4
import QtQuick.Controls 2.5
import QtQuick.Controls.Material 2.3

Item {
    id: meetingForm
    property alias createRoom: createRoom
    property alias notice: notice
    property alias accept: accept
    property alias reject: reject
    property alias meetingForm: meetingForm
    property alias btnLogout: btnLogout
    anchors.fill: parent

    Button {
        id: btnLogout
        width: 100
        height: 30
        anchors.top: parent.top
        anchors.right: parent.right
        anchors.topMargin: 10
        anchors.rightMargin: 15
        text: "立即登出"
    }

    Column {
        anchors.centerIn: parent
        height: 130
        width: 190
        spacing: 35

        TextField {
            id: createRoomInput
            width: parent.width
            horizontalAlignment: Text.AlignHCenter
            placeholderText: "输入房间名"
            text: channelManager.createChannelName
            validator: RegExpValidator {
                regExp: /^\d{1,8}$/
            }
            onTextChanged: channelManager.createChannelName = text
        }

        Button {
            id: createRoom
            width: parent.width
            text: "创建房间"
            highlighted: true
            Material.accent: Material.LightBlue
        }
    }

    Rectangle {
        width: 400
        height: 30
        anchors.bottom: parent.bottom
        anchors.bottomMargin: 40
        anchors.horizontalCenter: parent.horizontalCenter
        visible: meetingForm.state === "receivedInvitation"
        color: "#00000000"

        Label {
            id: notice
            anchors.left: parent.left
            anchors.verticalCenter: parent.verticalCenter
            text: ""
        }

        Button {
            id: accept
            height: 30
            anchors.verticalCenter: parent.verticalCenter
            anchors.right: reject.left
            anchors.rightMargin: 15
            text: "加入"
        }

        Button {
            id: reject
            height: 30
            anchors.verticalCenter: parent.verticalCenter
            anchors.right: parent.right
            text: "拒绝"
        }
    }
}




/*##^## Designer {
    D{i:0;autoSize:true;height:480;width:640}
}
 ##^##*/
