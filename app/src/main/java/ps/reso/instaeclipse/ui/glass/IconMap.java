package ps.reso.instaeclipse.ui.glass;

import ps.reso.instaeclipse.R;

/** Maps the shared (in-Instagram sheet) Material icons to the companion's thin Phosphor set. */
public final class IconMap {
    private IconMap() { }

    public static int ui(int res) {
        if (res == R.drawable.ic_arrow_back) return R.drawable.ic_ui_arrow_back;
        if (res == R.drawable.ic_at) return R.drawable.ic_ui_at;
        if (res == R.drawable.ic_block) return R.drawable.ic_ui_block;
        if (res == R.drawable.ic_camera) return R.drawable.ic_ui_camera;
        if (res == R.drawable.ic_cancel) return R.drawable.ic_ui_cancel;
        if (res == R.drawable.ic_chat) return R.drawable.ic_ui_chat;
        if (res == R.drawable.ic_check_circle) return R.drawable.ic_ui_check_circle;
        if (res == R.drawable.ic_chevron_right) return R.drawable.ic_ui_chevron_right;
        if (res == R.drawable.ic_close) return R.drawable.ic_ui_close;
        if (res == R.drawable.ic_content_copy) return R.drawable.ic_ui_content_copy;
        if (res == R.drawable.ic_delete) return R.drawable.ic_ui_delete;
        if (res == R.drawable.ic_download) return R.drawable.ic_ui_download;
        if (res == R.drawable.ic_error) return R.drawable.ic_ui_error;
        if (res == R.drawable.ic_expand_less) return R.drawable.ic_ui_expand_less;
        if (res == R.drawable.ic_expand_more) return R.drawable.ic_ui_expand_more;
        if (res == R.drawable.ic_eye) return R.drawable.ic_ui_eye;
        if (res == R.drawable.ic_eye_off) return R.drawable.ic_ui_eye_off;
        if (res == R.drawable.ic_folder) return R.drawable.ic_ui_folder;
        if (res == R.drawable.ic_gallery) return R.drawable.ic_ui_gallery;
        if (res == R.drawable.ic_heart) return R.drawable.ic_ui_heart;
        if (res == R.drawable.ic_home_edit) return R.drawable.ic_ui_edit;
        if (res == R.drawable.ic_info) return R.drawable.ic_ui_info;
        if (res == R.drawable.ic_jump_first) return R.drawable.ic_ui_jump_first;
        if (res == R.drawable.ic_link) return R.drawable.ic_ui_link;
        if (res == R.drawable.ic_live) return R.drawable.ic_ui_live;
        if (res == R.drawable.ic_logs_refresh) return R.drawable.ic_ui_refresh;
        if (res == R.drawable.ic_movie) return R.drawable.ic_ui_movie;
        if (res == R.drawable.ic_notification) return R.drawable.ic_ui_notification;
        if (res == R.drawable.ic_palette) return R.drawable.ic_ui_palette;
        if (res == R.drawable.ic_pin) return R.drawable.ic_ui_pin;
        if (res == R.drawable.ic_restart) return R.drawable.ic_ui_restart;
        if (res == R.drawable.ic_save) return R.drawable.ic_ui_save;
        if (res == R.drawable.ic_search) return R.drawable.ic_ui_search;
        if (res == R.drawable.ic_settings_gear) return R.drawable.ic_ui_settings_gear;
        if (res == R.drawable.ic_shield) return R.drawable.ic_ui_shield;
        if (res == R.drawable.ic_sparkle) return R.drawable.ic_ui_sparkle;
        if (res == R.drawable.ic_story_ring) return R.drawable.ic_ui_story_ring;
        if (res == R.drawable.ic_terminal) return R.drawable.ic_ui_terminal;
        if (res == R.drawable.ic_timer) return R.drawable.ic_ui_timer;
        if (res == R.drawable.ic_tune) return R.drawable.ic_ui_tune;
        if (res == R.drawable.ic_upload) return R.drawable.ic_ui_upload;
        return res;
    }
}
