package com.v2ray.ang.ui.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.enums.NetworkType
import com.v2ray.ang.ui.compose.FormDropdownField
import com.v2ray.ang.ui.compose.FormTextField
import com.v2ray.ang.ui.compose.SettingsSwitchItem
import java.util.UUID

class ServerTuicActivity : BaseServerActivity() {

    override val serverConfigType: EConfigType = EConfigType.TUIC

    @Composable
    override fun ScreenContent() {
        val uiState = rememberSaveable(saver = ServerUiState.Saver) {
            ServerUiState.from(
                initialConfig = initialConfig
            )
        }.apply {
            configType = EConfigType.TUIC
        }

        ServerEditorScaffold(
            title = serverConfigType.toString(),
            onSaveClick = { saveServer(uiState) }
        ) {
            CommonBasicFields(uiState)
            TuicProtocolFields(uiState)
        }
    }

    override fun validateCommonConfig(state: ServerUiState, config: ProfileItem): Boolean {
        // TUIC authenticates with a UUID (username field) plus a password; the core rejects a malformed UUID.
        state.isUsernameError = runCatching { UUID.fromString(config.username.orEmpty().trim()) }.isFailure
        if (state.isUsernameError) {
            return false
        }
        config.username = config.username.orEmpty().trim()
        return super.validateCommonConfig(state, config)
    }

    override fun validateProtocolConfig(config: ProfileItem): Boolean {
        // TUIC always runs over QUIC with TLS; "h3" is the conventional ALPN.
        config.network = NetworkType.TUIC.type
        config.security = AppConfig.TLS
        if (config.alpn.isNullOrBlank()) {
            config.alpn = "h3"
        }
        return true
    }

    @Composable
    private fun TuicProtocolFields(state: ServerUiState) {
        FormTextField(
            label = stringResource(R.string.server_lab_id),
            value = state.username,
            onValueChange = { state.username = it },
            isError = state.isUsernameError
        )
        FormTextField(
            label = stringResource(R.string.server_lab_id3),
            value = state.password,
            onValueChange = { state.password = it },
            isError = state.isPasswordError
        )
        FormDropdownField(
            stringResource(R.string.server_lab_congestion_control),
            state.congestionControl,
            listOf("", "cubic", "bbr", "new_reno"),
            { state.congestionControl = it }
        )
        FormDropdownField(
            stringResource(R.string.server_lab_udp_relay_mode),
            state.udpRelayMode,
            listOf("", "native", "quic"),
            { state.udpRelayMode = it }
        )
        SettingsSwitchItem(
            title = stringResource(R.string.server_lab_zero_rtt),
            checked = state.zeroRtt,
            onCheckedChange = { state.zeroRtt = it }
        )

        SettingsSwitchItem(
            title = stringResource(R.string.server_lab_allow_insecure),
            checked = state.allowInsecure,
            onCheckedChange = { state.allowInsecure = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_sni),
            state.sni,
            { state.sni = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_stream_alpn),
            state.alpn,
            { state.alpn = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_ech_config_list),
            state.echConfigList,
            { state.echConfigList = it }
        )
        FormTextField(
            stringResource(R.string.server_lab_pinned_ca256),
            state.pinnedCA256,
            { state.pinnedCA256 = it }
        )
    }
}
