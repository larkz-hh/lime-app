package xyz.larkzhh.lime.ui.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import xyz.larkzhh.lime.R
import xyz.larkzhh.lime.navigation.Screen
import xyz.larkzhh.lime.ui.theme.LimePageBg
import xyz.larkzhh.lime.ui.theme.LimeTextMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    navController: NavHostController,
    viewModel: CreateGroupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.createdGroupId) {
        state.createdGroupId?.let { groupId ->
            viewModel.clearCreatedGroupId()
            navController.navigate(Screen.ImChat.createRoute("group_$groupId")) {
                popUpTo(Screen.GroupList.route)
            }
        }
    }

    Scaffold(
        containerColor = LimePageBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.msg_create_group),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = LimeTextMain,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = LimeTextMain,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LimePageBg,
                    scrolledContainerColor = Color.Unspecified,
                    navigationIconContentColor = Color.Unspecified,
                    titleContentColor = Color.Unspecified,
                    actionIconContentColor = Color.Unspecified
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            GroupFieldCard(
                label = stringResource(R.string.group_name),
                required = true,
                value = state.name,
                onValueChange = viewModel::onNameChange,
                placeholder = stringResource(R.string.group_name_hint),
                maxLength = 24,
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))
            GroupFieldCard(
                label = stringResource(R.string.group_create_intro),
                required = true,
                value = state.introduction,
                onValueChange = viewModel::onIntroChange,
                placeholder = stringResource(R.string.group_create_intro_hint),
                maxLength = 100,
                singleLine = false,
                minLines = 3,
            )
            state.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = viewModel::create,
                enabled = !state.isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                if (state.isCreating) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.height(20.dp),
                    )
                } else {
                    Text(stringResource(R.string.group_create_action), fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
