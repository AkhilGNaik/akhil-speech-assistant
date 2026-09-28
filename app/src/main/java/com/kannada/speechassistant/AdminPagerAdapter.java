package com.kannada.speechassistant;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminPagerAdapter extends FragmentStateAdapter {

    public AdminPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new UsersFragment();
            case 1:
                return new EmergenciesFragment();
            case 2:
                return new ConnectionsFragment();
            case 3:
                return new CommunicationFragment();
            default:
                return new UsersFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
