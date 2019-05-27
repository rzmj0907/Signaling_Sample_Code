#ifndef MAIN_H_
#define MAIN_H_

#include <QFile>
#include <QString>
#include <QDateTime>
#include <QQuickStyle>
#include <QQmlContext>
#include <QGuiApplication>
#include <QQmlApplicationEngine>

void outputMessage(QtMsgType type, const QMessageLogContext &context, const QString &msg);

#endif // MAIN_H_
