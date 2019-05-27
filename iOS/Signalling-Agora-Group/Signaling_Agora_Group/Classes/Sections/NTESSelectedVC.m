//
//  NTESSelectedVC.m
//  Signalling_Agora_Group
//
//  Created by Netease on 2019/5/10.
//  Copyright © 2019 Netease. All rights reserved.
//

#import "NTESSelectedVC.h"
#define NTES_MAX_INVITES (1)
@interface NTESSelectedVC ()<NIMSignalManagerDelegate>
@property (weak, nonatomic) IBOutlet UITextField *roomIdInput;
@property (weak, nonatomic) IBOutlet UILabel *callInfoLab;
@property (weak, nonatomic) IBOutlet UIView *callInfoView;
@property (weak, nonatomic) IBOutlet UIButton *createRoomBtn;

@property (strong, nonatomic) NIMSignalingInviteNotifyInfo *invite;
@end

@implementation NTESSelectedVC

- (void)dealloc {
    [[NIMSDK sharedSDK].signalManager removeDelegate:self];
}

- (void)viewDidLoad {
    [super viewDidLoad];
    _createRoomBtn.layer.borderColor = [UIColor blackColor].CGColor;
    UIImage *image = [self imageWithColor:[UIColor blueColor]];
    [_createRoomBtn setBackgroundImage:image forState:UIControlStateHighlighted];
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [super touchesBegan:touches withEvent:event];
    [self.view endEditing:YES];
}

- (void)viewDidAppear:(BOOL)animated {
    [super viewDidAppear:animated];
    _invite = nil;
    _callInfoView.hidden = YES;
    [NTESDataCenter shareIntance].channelInfo = nil;
    [[NIMSDK sharedSDK].signalManager addDelegate:self];
}

- (void)viewDidDisappear:(BOOL)animated {
    [super viewDidDisappear:animated];
    [self.view endEditing:YES];
    [[NIMSDK sharedSDK].signalManager removeDelegate:self];
}

- (IBAction)loginOutAction:(id)sender {
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].loginManager logout:^(NSError * _Nullable error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"注销失败。error:%ld", (long)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        } else {
            [NTESDataCenter shareIntance].account = nil;
            [NTESDataCenter shareIntance].token = nil;
            [weakSelf.navigationController popViewControllerAnimated:YES];
        }
    }];
}

- (IBAction)createRoomAction:(id)sender {
    NSString *roomName = _roomIdInput.text;
    if (roomName.length == 0) {
        [self.view makeToast:@"房间号为空" duration:2 position:CSToastPositionCenter];
    }
    [self doCreateWithChannelName:roomName];
}

- (IBAction)joinAction:(id)sender {
    [self doAcceptWithInvite:_invite];
}

- (IBAction)rejectAction:(id)sender {
    [self doRejectWithInvite:_invite];
}

- (void)showInvite:(NIMSignalingInviteNotifyInfo *)info {
    _callInfoLab.text = [NSString stringWithFormat:@"%@邀请你加入房间", info.fromAccountId];
    _callInfoView.hidden = NO;
}

- (void)showMeetingVC {
    UIStoryboard *storyboard = [UIStoryboard storyboardWithName:@"Main" bundle:nil];
    UIViewController *vc = [storyboard instantiateViewControllerWithIdentifier:@"NTESMeetingVC"];
    [self.navigationController pushViewController:vc animated:YES];
}

#pragma mark - Function
- (void)doCreateWithChannelName:(NSString *)roomName {
    __weak typeof(self) weakSelf = self;
    NIMSignalingCreateChannelRequest *request = [[NIMSignalingCreateChannelRequest alloc] init];
    request.channelName = roomName;
    request.channelType = NIMSignalingChannelTypeVideo;
    [[NIMSDK sharedSDK].signalManager signalingCreateChannel:request completion:^(NSError * _Nullable error, NIMSignalingChannelInfo * _Nullable response) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"创建房间失败。error:%ld", (long)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        } else {
            [weakSelf doJoinWithChannelId:response.channelId completion:^(NSError *error) {
                if (error) {
                    NSString *msg = [NSString stringWithFormat:@"加入房间失败。error:%ld", (long)error.code];
                    [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
                    [weakSelf doDestoryWithChannelId:response.channelId];
                } else {
                    [weakSelf showMeetingVC];
                }
            }];
        }
    }];
}

- (void)doJoinWithChannelId:(NSString *)channelId completion:(NTESSignalBlock)completion {
    NIMSignalingJoinChannelRequest *req = [[NIMSignalingJoinChannelRequest alloc] init];
    req.channelId = channelId;
    req.uid = [[NTESDataCenter shareIntance] createRandom32BitUid];
    [[NIMSDK sharedSDK].signalManager signalingJoinChannel:req completion:^(NSError * _Nullable error, NIMSignalingChannelDetailedInfo * _Nullable response) {
        if (!error) {
            [NTESDataCenter shareIntance].channelInfo = response;
        }
        if (completion) {
            completion(error);
        }
    }];
}

- (void)doDestoryWithChannelId:(NSString *)channelId {
    NIMSignalingCloseChannelRequest *req = [[NIMSignalingCloseChannelRequest alloc] init];
    req.channelId = channelId;
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingCloseChannel:req completion:^(NSError * _Nullable error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"关闭房间失败。error:%ld", (long)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        }
    }];
}

- (void)doRejectWithInvite:(NIMSignalingInviteNotifyInfo *)invite {
    NIMSignalingRejectRequest *req = [[NIMSignalingRejectRequest alloc] init];
    req.channelId = invite.channelInfo.channelId;
    req.accountId = invite.fromAccountId;
    req.requestId = invite.requestId;
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingReject:req completion:^(NSError * _Nullable error) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"拒绝%@失败。error:%ld", req.accountId, (long)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        } else {
            if ([weakSelf.invite.fromAccountId isEqualToString:invite.fromAccountId]) {
                weakSelf.invite = nil;
                weakSelf.callInfoView.hidden = YES;
            }
        }
    }];
}

- (void)doAcceptWithInvite:(NIMSignalingInviteNotifyInfo *)invite {
    NIMSignalingAcceptRequest *req = [[NIMSignalingAcceptRequest alloc] init];
    req.channelId = invite.channelInfo.channelId;
    req.accountId = invite.fromAccountId;
    req.requestId = invite.requestId;
    req.autoJoin = YES;
    req.uid = [[NTESDataCenter shareIntance] createRandom32BitUid];
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingAccept:req completion:^(NSError * _Nullable error, NIMSignalingChannelDetailedInfo * _Nullable response) {
        if (error) {
            NSString *msg = [NSString stringWithFormat:@"接受%@失败。error:%ld", req.accountId, (long)error.code];
            [weakSelf.view makeToast:msg duration:2 position:CSToastPositionCenter];
        } else {
            if ([weakSelf.invite.fromAccountId isEqualToString:invite.fromAccountId]) {
                weakSelf.invite = nil;
                weakSelf.callInfoView.hidden = YES;
                [NTESDataCenter shareIntance].channelInfo = response;
                [weakSelf showMeetingVC];
            }
        }
    }];
}

#pragma mark - <NIMSignalManagerDelegate>
- (void)nimSignalingOnlineNotifyEventType:(NIMSignalingEventType)eventType
                                 response:(NIMSignalingNotifyInfo *)notifyResponse {
    switch (eventType) {
        case NIMSignalingEventTypeInvite:
        {
            NIMSignalingInviteNotifyInfo *info = (NIMSignalingInviteNotifyInfo *)notifyResponse;
            if (!_invite) {
                _invite = info;
                [self showInvite:info];
            } else {
                [self doRejectWithInvite:info];
            }
            break;
        }
        case NIMSignalingEventTypeCancelInvite:
        {
            NIMSignalingCancelInviteNotifyInfo *info = (NIMSignalingCancelInviteNotifyInfo *)notifyResponse;
            if ([_invite.fromAccountId isEqualToString:info.fromAccountId]) {
                _callInfoView.hidden = YES;
            }
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

- (UIImage *)imageWithColor:(UIColor *)color {
    CGFloat alphaChannel;
    [color getRed:NULL green:NULL blue:NULL alpha:&alphaChannel];
    BOOL opaqueImage = (alphaChannel == 1.0);
    CGRect rect = CGRectMake(0, 0, 1, 1);
    UIGraphicsBeginImageContextWithOptions(rect.size, opaqueImage, [UIScreen mainScreen].scale);
    [color setFill];
    UIRectFill(rect);
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return image;
}

@end
