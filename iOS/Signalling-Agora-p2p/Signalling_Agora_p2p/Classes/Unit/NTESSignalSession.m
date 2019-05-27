//
//  NTESSignalSession.m
//  Signalling_Agora_p2p
//
//  Created by Netease on 2019/5/9.
//  Copyright © 2019年 Netease. All rights reserved.
//

#import "NTESSignalSession.h"
#import "NSString+NTES.h"

typedef NS_ENUM (NSInteger, NTESSignalProcessState){
    NTESSignalProcessStateIdle,
    NTESSignalProcessStateInteraction,
    NTESSignalProcessStateCompletion,
};

@interface NTESSignalSession ()

@property (nonatomic, copy) NSString *channelId;
@property (nonatomic, copy) NSString *accountId;
@property (nonatomic, copy) NSString *requestId;
@property (nonatomic, assign) NTESSignalProcessState processState;

@end

@implementation NTESSignalSession

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)doCallWithAccountId:(NSString *)accountId
                 completion:(nullable NTESSignalBlock)completion {
    _accountId = accountId;
    _requestId = [NSString randomCallRequestId];
    _processState = NTESSignalProcessStateInteraction;
    NIMSignalingCallRequest *req = [[NIMSignalingCallRequest alloc] init];
    req.accountId = _accountId;
    req.requestId = _requestId;
    req.channelName = [NSString randomChannelName];
    req.channelType = NIMSignalingChannelTypeVideo;
    req.uid = [[NTESDataCenter shareIntance] createRandom32BitUid];
    
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingCall:req completion:^(NSError * _Nullable error, NIMSignalingChannelDetailedInfo * _Nullable response) {
        if (error) {
            weakSelf.processState = NTESSignalProcessStateIdle;
        } else {
            [NTESDataCenter shareIntance].channelInfo = response;
            weakSelf.channelId = response.channelId;
            [weakSelf getUidFromMembers:response.members];
        }
        if (completion) {
            completion(error);
        }
    }];
}

- (void)doAcceptCompletion:(NTESSignalBlock)completion {
    NIMSignalingAcceptRequest *req = [[NIMSignalingAcceptRequest alloc] init];
    req.channelId = _channelId;
    req.accountId = _accountId;
    req.requestId = _requestId;
    req.uid = [[NTESDataCenter shareIntance] createRandom32BitUid];
    req.autoJoin = YES;
    
    __weak typeof(self) weakSelf = self;
    [[NIMSDK sharedSDK].signalManager signalingAccept:req
                                           completion:^(NSError * _Nullable error, NIMSignalingChannelDetailedInfo * _Nullable response) {
        if (error) {
            weakSelf.processState = NTESSignalProcessStateIdle;
        } else {
            weakSelf.processState = NTESSignalProcessStateCompletion;
            [NTESDataCenter shareIntance].channelInfo = response;
            [weakSelf getUidFromMembers:response.members];
        }
        if (completion) {
            completion(error);
        }
    }];
}

- (void)doRejectCompletion:(NTESSignalBlock)completion {
    _processState = NTESSignalProcessStateIdle;
    NIMSignalingRejectRequest *req = [[NIMSignalingRejectRequest alloc] init];
    req.channelId = _channelId;
    req.accountId = _accountId;
    req.requestId = _requestId;
    [[NIMSDK sharedSDK].signalManager signalingReject:req completion:^(NSError * _Nullable error) {
        if (completion) {
            completion(error);
        }
    }];
}

- (void)doRejectWithChannelId:(NSString *)channelId
                    accountId:(NSString *)accountId
                    requestId:(NSString *)requestId {
    NIMSignalingRejectRequest *req = [[NIMSignalingRejectRequest alloc] init];
    req.channelId = channelId;
    req.accountId = accountId;
    req.requestId = requestId;
    [[NIMSDK sharedSDK].signalManager signalingReject:req completion:^(NSError * _Nullable error) {
        if (error) {
            NSLog(@"reject error:%@", error);
        }
    }];
}

- (void)doCloseChannelCompletion:(NTESSignalBlock)completion {
    
    if (_processState == NTESSignalProcessStateIdle) {
        return;
    }
    _processState = NTESSignalProcessStateIdle;
    NIMSignalingCloseChannelRequest *req = [[NIMSignalingCloseChannelRequest alloc] init];
    req.channelId = _channelId;
    [[NIMSDK sharedSDK].signalManager signalingCloseChannel:req completion:^(NSError * _Nullable error) {
        if (error) {
            NSLog(@"close error:%@", error);
        }
        if (completion) {
            completion(error);
        }
    }];
}

#pragma mark - Private
- (void)getUidFromMembers:(NSArray<NIMSignalingMemberInfo *> *)members {
    NSString *myAccount = [NTESDataCenter shareIntance].account;
    [members enumerateObjectsUsingBlock:^(NIMSignalingMemberInfo * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
        if ([obj.accountId isEqualToString:myAccount]) {
            [NTESDataCenter shareIntance].uid = (NSUInteger)obj.uid;
            *stop = YES;
        }
    }];
}

#pragma mark - NIMSignalManagerDelegate
- (void)nimSignalingOnlineNotifyEventType:(NIMSignalingEventType)eventType
                                 response:(NIMSignalingNotifyInfo *)notifyResponse {
    switch (eventType) {
        case NIMSignalingEventTypeJoin: {
            if (_processState != NTESSignalProcessStateInteraction) {
                return;
            }
            _processState = NTESSignalProcessStateCompletion;
            if (_delegate && [_delegate respondsToSelector:@selector(didJoinChannel)]) {
                [_delegate didJoinChannel];
            }
            break;
        }
            
        case NIMSignalingEventTypeInvite:{
            NIMSignalingInviteNotifyInfo *info = (NIMSignalingInviteNotifyInfo *)notifyResponse;
            if (_processState != NTESSignalProcessStateIdle) { //拒绝
                [self doRejectWithChannelId:info.channelInfo.channelId
                                  accountId:info.fromAccountId
                                  requestId:info.requestId];
            } else {
                _processState = NTESSignalProcessStateInteraction;
                _accountId = info.fromAccountId;
                _requestId = info.requestId;
                _channelId = info.channelInfo.channelId;
                if (_delegate && [_delegate respondsToSelector:@selector(didReceiveInvite)]) {
                    [_delegate didReceiveInvite];
                }
            }
            break;
        }
        case NIMSignalingEventTypeCancelInvite: {
            if (_processState != NTESSignalProcessStateInteraction) {
                return;
            }
            _processState = NTESSignalProcessStateIdle;
            if (_delegate && [_delegate respondsToSelector:@selector(didCancelInvite)]) {
                [_delegate didCancelInvite];
            }
            break;
        }
        case NIMSignalingEventTypeReject: {
            if (_processState != NTESSignalProcessStateInteraction) {
                return;
            }
            _processState = NTESSignalProcessStateIdle;
            if (_delegate && [_delegate respondsToSelector:@selector(didRejectInvite)]) {
                [_delegate didRejectInvite];
            }
            break;
        }
        case NIMSignalingEventTypeClose:
        case NIMSignalingEventTypeLeave: {
            _processState = NTESSignalProcessStateIdle;
            [[NSNotificationCenter defaultCenter] postNotificationName:kDidChannelCloseNotication object:nil];
            [self doCloseChannelCompletion:nil];
        }
        break;
        case NIMSignalingEventTypeAccept: {
            break;
        }
        default:
            break;
    }
}

@end


