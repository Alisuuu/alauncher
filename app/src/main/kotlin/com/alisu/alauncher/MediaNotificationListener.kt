package com.alisu.alauncher

import android.service.notification.NotificationListenerService

/**
 * NotificationListenerService que permite ao launcher acessar MediaSessions
 * ativas (Spotify, YouTube Music, etc.) via MediaSessionManager.
 *
 * O usuário precisa habilitar este serviço em:
 * Configurações > Apps & Notificações > Acesso a notificações > Alauncher
 */
class MediaNotificationListener : NotificationListenerService()
