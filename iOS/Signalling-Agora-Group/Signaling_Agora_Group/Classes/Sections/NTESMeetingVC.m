//
//  NTESMeetingVC.m
//  Signalling_Agora_Group
//
//  Created by Netease on 2019/5/10.
//  Copyright © 2019 Netease. All rights reserved.
//

#import "NTESMeetingVC.h"
#import "NTESDisplayView.h"
#import "NSString+NTES.h"

@interface NTESMeetingVC ()<AgoraRtcEngineDelegate,UITextFieldDelegate, NIMSignalManagerDelegate>

@property (weak, nonatomic) IBOutlet NTESDisplayView *displayView;
@property (weak, nonatomic) IBOutlet UITextField *inviteInput;

@property (nonatomic, strong) NSString *agoraChannel;
@property (nonatomic, assign) NSUInteger agoraUid;
@property (nonatomic, strong) AgoraRtcEngineKit *agoraKit;

@end

@implementation NTESMeetingVC

- (void)dealloc {
    [[NIMSDK sharedSDK].signalManager removeDelegate:self];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    _inviteInput.delegate = self;
    [self initializeAgoraEngine];
    [self setupVideo];
    [self setupLocalVideo];
    [self joinMeeting];
    [[NIMSDK sharedSDK].signalManager addDelegate:self];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    [self.view endEditing:YES];
}

- (IBAction)videoMuteAction:(UIButton *)sender {
    sender.selected = !sender.selected;
    [_agoraKit muteLocalVideoStream:sender.selected];
    [_displayView setDisplayModelHidden:sender.selected
                                    uid:[NTESDataCenter shareIntance].myUid];
}

- (IBAction)audioMuteAction:(UIButton *)sender {
    sender.selected = !sender.selected;
    [_agoraKit muteLocalAudioStream:sender.selected];
}

- (IBAction)hungupAction:(id)sender {
    [_agoraKit leaveChannel:nil];
    
    if ([NTESDataCenter shareIntance].channelInfo.members.count == 1) {
        [self doCloseChannel];
    } else {
        [self doLeaveChannel];
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField endEditing:YES];
    [self doInivteWithAccount:textField.text];
    return YES;
}

#pragma mark - Agora Sdk
- (void)initializeAgoraEngine {
    _agoraKit = [AgoraRtcEngineKit sharedEngineWithAppId:NTES_DEMO_AGORA_APPID
                                                delegate:self];
}

- (void)setupVideo {
    [_agoraKit enableVideo];
    AgoraVideoEncoderConfiguration *config = [[AgoraVideoEncoderConfiguration alloc] initWithSize:AgoraVideoDimension640x360 frameRate:AgoraVideoFrameRateFps15 bitrate:AgoraVideoBitrateStandard orientationMode:AgoraVideoOutputOrientationModeAdaptative];
    [_agoraKit setVideoEncoderConfiguration:config];
}

- (void)setupLocalVideo {
    NSUInteger myUid = [[NTESDataCenter shareIntance] myUid];
    NTESDisplayModel *displayModel = [[NTESDisplayModel alloc] init];
    displayModel.uid = myUid;
    displayModel.account = [[NTESDataCenter shareIntance] account];
    [_displayView addDisplayModel:displayModel];
    
    AgoraRtcVideoCanvas *canvas = [[AgoraRtcVideoCanvas alloc] init];
    canvas.view = displayModel.view;
    canvas.renderMode = AgoraVideoRenderModeHidden;
    canvas.uid = myUid;
    [_agoraKit setupLocalVideo:canvas];
}

- (void)setupRemoteVideoWithUid:(NSUInteger)uid {
    NTESDisplayModel *displayModel = [[NTESDisplayModel alloc] init];
    displayModel.uid = uid;
    displayModel.account = [[NTESDataCenter shareIntance] accountWithUid:uid];
    [_displayView addDisplayModel:displayModel];
    
    AgoraRtcVideoCanvas *canvas = [[AgoraRtcVideoCanvas alloc] init];
    canvas.view = displayModel.view;
    canvas.renderMode = AgoraVideoRenderModeHidden;
    canvas.uid = uid;
    [_agoraKit setupRemoteVideo:canvas];
}

- (void)joinMeeting {
    [_agoraKit setDefaultAudioRouteToSpeakerphone:YES];
    [_agoraKit setChannelProfile:AgoraChannelProfileCommunication];
    NSString *channnelId = [NTESDataCenter shareIntance].channelInfo.channelName;
    NSUInteger uid = [NTESDataCenter shareIntance].myUid;
    
    __weak typeof(self) weakSelf = self;
    int ret = [_agoraKit joinChannelByToken:nil channelId:channnelId info:nil uid:uid joinSuccess:^(NSString * _Nonnull channel, NSUInteger uid, NSInteger elapsed) {
        weakSelf.agoraChannel = channnelId;
        weakSelf.agoraUid = uid;
    }];
    if (ret < 0) {
        NSString *msg = [NSString stringWithFormat:@"加入会议失败, error:%d", ret];
        [[UIApplication sharedApplication].keyWindow makeToast:msg
                                                      duration:2
                                                      position:CSToastPositionCenter];
    }
}

#pragma mark - <AgoraRtcEngineDelegate>
- (void)rtcEngine:(AgoraRtcEngineKit * _Nonnull)engine firstRemoteVideoDecodedOfUid:(NSUInteger)uid size:(CGSize)size elapsed:(NSInteger)elapsed {
    [self setupRemoteVideoWithUid:uid];
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didOfflineOfUid:(NSUInteger)uid reason:(AgoraUserOfflineReason)reason {
    [_displayView setDisplayModelHidden:YES uid:uid];
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didVideoMuted:(BOOL)muted byUid:(NSUInteger)uid {
    [_displayView setDisplayModelHidden:muted uid:uid];
}

- (void)rtcEngine:(AgoraRtcEngineKit *)engine didAudioMuted:(BOOL)muted byUid:(NSUInteger)uid {
    NSString *msg = [NSString stringWithFormat:@"远端音频%@", muted ? @"关闭":@"开启"];
    [self.view makeToast:msg duration:2 position:CSToastPositionCenter];
}

#pragma mark - NIMSdk
- (void)doCloseChannel {
    NSString *channelId = [NTESDataCenter shareIntance].channelInfo.channelId;
    NIMSignalingCloseChannelRequest *req = [[NIMSignalingCloseChannelRequest alloc] init];
    req.channelId = channelId;
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingCloseChannel:req completion:^(NSError * _Nullable error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"关闭房间失败。error:%ld", (long)error.code];
            UIWindow *keyWindow = [UIApplication sharedApplication].keyWindow;
            [keyWindow makeToast:msg duration:2 position:CSToastPositionCenter];
        }
        [NTESDataCenter shareIntance].channelInfo = nil;
        [weakSelf.navigationController popViewControllerAnimated:YES];
    }];
}

- (void)doLeaveChannel {
    NSString *channelId = [NTESDataCenter shareIntance].channelInfo.channelId;
    NIMSignalingLeaveChannelRequest *req = [[NIMSignalingLeaveChannelRequest alloc] init];
    req.channelId = channelId;
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingLeaveChannel:req completion:^(NSError * _Nullable error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"离开房间失败。error:%ld", (long)error.code];
            UIWindow *keyWindow = [UIApplication sharedApplication].keyWindow;
            [keyWindow makeToast:msg duration:2 position:CSToastPositionCenter];
        }
        [NTESDataCenter shareIntance].channelInfo = nil;
        [weakSelf.navigationController popViewControllerAnimated:YES];
    }];
}

- (void)doInivteWithAccount:(NSString *)account {
    NIMSignalingInviteRequest *req = [[NIMSignalingInviteRequest alloc] init];
    req.channelId = [NTESDataCenter shareIntance].channelInfo.channelId;
    req.requestId = [NSString randomCallRequestId];
    req.accountId = account;
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingInvite:req completion:^(NSError * _Nullable error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"邀请失败。error:%ld", (long)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        }
    }];
}

#pragma mark - <NIMSignalManagerDelegate>
- (void)nimSignalingOnlineNotifyEventType:(NIMSignalingEventType)eventType
                                 response:(NIMSignalingNotifyInfo *)notifyResponse {
    switch (eventType) {
        case NIMSignalingEventTypeClose:
        {
            [_agoraKit leaveChannel:nil];
            UIWindow *keyWindow = [UIApplication sharedApplication].keyWindow;
            [keyWindow makeToast:@"房间已关闭" duration:2 position:CSToastPositionCenter];
            [self.navigationController popViewControllerAnimated:YES];
            break;
        }
        case NIMSignalingEventTypeJoin:
        {
            break;
        }
        case NIMSignalingEventTypeLeave:
        {
            NIMSignalingLeaveNotifyInfo *info = (NIMSignalingLeaveNotifyInfo *)notifyResponse;
            [_displayView removeDisplayModeWithAccount:info.fromAccountId];
            break;
        }
        case NIMSignalingEventTypeReject:
        {
            NIMSignalingRejectNotifyInfo *info = (NIMSignalingRejectNotifyInfo *)notifyResponse;
            NSString *msg = [NSString stringWithFormat:@"%@ 拒绝了你的邀请", info.fromAccountId];
            [self.view makeToast:msg duration:2 position:CSToastPositionCenter];
            break;
        }
        default:
            break;
    }
}

@end
