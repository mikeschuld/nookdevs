#!/usr/bin/perl

#
# Converts the Nook "buttons.svg" vector image to individual files and copies
# these into the respective projects' source folders.
#
# This script is in the public domain -- distribute and modify as you please.
#
# Requirements (in $PATH):
#   - Linux
#   - Inkscape (inkscape)
#   - ImageMagick (convert)
#   - rm, tempfile
#

use 5.010;

use integer;
use strict;
use warnings;

use File::Basename qw(basename dirname);
use File::Copy qw(copy);
require Getopt::Long;


# constants...
$::BASEDIR = dirname $0;
$::SVG = "$::BASEDIR/buttons.svg";

$::BUTTON_WIDTH = 96;
$::BUTTON_HEIGHT = 144;
$::COUNTX = 10;
$::COUNTY = 3;

$::NOOKDEVS = "$ENV{HOME}/git/nookdevs";
$::NOOKAPPS = "$ENV{HOME}/git/nookapps";
$::NOOKAPP_GOOGLEREADER = "$ENV{HOME}/git/nookapp-googlereader";

%::MAPPING = (  # button index => [ name, normal PNG path, pressed PNG path, [ normal PNG path, pressed PNG path, [...] ] ]
  0 => [ "bn_home",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_home.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_home_focus.png" ],
  1 => [ "bn_reading_now",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_readingnow.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_readingnow_focus.png" ],
  2 => [ "bn_my_library",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_mylibrary.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_mylibrary_focus.png" ],
  3 => [ "bn_the_daily",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_thedaily.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_thedaily_focus.png" ],
  4 => [ "bn_shop",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_shop.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_shop_focus.png" ],
  5 => [ "bn_web",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_browser.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_browser_focus.png",
         "$::NOOKDEVS/nookBrowser/res/drawable/nookbrowser.png", "$::NOOKDEVS/nookBrowser/res/drawable/nookbrowser_focus.png" ],
  6 => [ "bn_settings",
          "$::NOOKDEVS/nookLauncher/res/drawable/bn_settings.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_settings_focus.png" ],
  7 => [ "bn_wifi",
          "$::NOOKDEVS/nookLauncher/res/drawable/bn_wifi.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_wifi_focus.png" ],
  8 => [ "bn_chess",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_chess.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_chess_focus.png" ],
  9 => [ "bn_sudoku",
         "$::NOOKDEVS/nookLauncher/res/drawable/bn_sudoku.png", "$::NOOKDEVS/nookLauncher/res/drawable/bn_sudoku_focus.png" ],
  10 => [ "na_trook",
          "$::NOOKAPPS/trook/res/drawable/trook_icon.png", "$::NOOKAPPS/trook/res/drawable/trook_icon_selected.png" ],
  11 => [ "nd_files",
          "$::NOOKDEVS/nookFileManager/res/drawable/filemgr_normal.PNG", "$::NOOKDEVS/nookFileManager/res/drawable/filemgr_pressed.PNG" ],
  12 => [ "nd_my_books",
          "$::NOOKDEVS/nookLibrary/res/drawable/mybooks.png", "$::NOOKDEVS/nookLibrary/res/drawable/mybooks_focus.png" ],
  13 => [ "nd_default_launcher",
          "$::NOOKDEVS/nookLauncher/res/drawable/nd_defaultlauncher.png", "$::NOOKDEVS/nookLauncher/res/drawable/nd_defaultlauncher_focus.png" ],
  14 => [ "na_nooklets",
          "$::NOOKAPPS/nookletcontainer/res/drawable/nooklet_icon.png", "$::NOOKAPPS/nookletcontainer/res/drawable/nooklet_icon_selected.png" ],
  15 => [ "nd_browser",
          "$::NOOKDEVS/nookBrowser/res/drawable/nookbrowser.png", "$::NOOKDEVS/nookBrowser/res/drawable/nookbrowser_focus.png" ],
  16 => [ "nd_launcher_settings",
          "$::NOOKDEVS/nookLauncher/res/drawable/nd_launchersettings.png", "$::NOOKDEVS/nookLauncher/res/drawable/nd_launchersettings_focus.png" ],
  17 => [ "nd_wifi_locker",
          "$::NOOKDEVS/nookWifiLocker/res/drawable/wifi_normal.PNG", "$::NOOKDEVS/nookWifiLocker/res/drawable/wifi_pressed.PNG" ],
  18 => [ "folder_games",
          "/dev/null", "/dev/null" ],
  19 => [ "nd_crosswords",
          "/dev/null", "/dev/null" ],
  20 => [ "cc_media",
          "$::NOOKDEVS/nookMedia/res/drawable/cc_media.png", "$::NOOKDEVS/nookMedia/res/drawable/cc_media_focus.png" ],
  21 => [ "nd_market",
          "$::NOOKDEVS/nookMarket/res/drawable/nd_market.jpg", "$::NOOKDEVS/nookMarket/res/drawable/nd_market_sel.jpg" ],
  22 => [ "nd_calculator",
          "$::NOOKDEVS/nookCalculator/res/drawable/icon.png", "$::NOOKDEVS/nookCalculator/res/drawable/icon_pressed.png" ],
  23 => [ "mz_googlereader",
          "$::NOOKAPP_GOOGLEREADER/res/drawable/icon.png", "$::NOOKAPP_GOOGLEREADER/res/drawable/icon_pressed.png" ],
  24 => [ "nd_tasks",
          "$::NOOKDEVS/nookTaskManager/res/drawable/icon.png", "$::NOOKDEVS/nookTaskManager/res/drawable/icon_pressed.png" ],
  # 25
  # 26
  # 27
  28 => [ "folder_template",
          "/dev/null", "/dev/null" ],
  29 => [ "nd_notes",
          "$::NOOKDEVS/nookNotes/res/drawable/nd_notes.png", "$::NOOKDEVS/nookNotes/res/drawable/nd_notes_focus.png" ],
);

# parse command-line arguments, validate context...
my %opts = ();
&Getopt::Long::Configure(qw(bundling));
my $ok = &Getopt::Long::GetOptions(\%opts, qw(
  dir|d=s file|f=s
  help|h
));
exit -1 unless $ok;
if ($opts{help} || @ARGV) {
    die<<EOT;
usage: @{[basename $0]} [OPTION[S]] APPLICATION[S]

options:
  --dir DIR, -d DIR      simply output files to the specified directory,
                         creating the directory or overwriting existing files
  --file FILE, -f FILE   use an SVG file other than the default

  --help, -h    show this help
EOT
}
die "“$::SVG” not found.  Symlink one of the buttons SVGs there.\n" unless -r $::SVG || $opts{file};
die "Failed to create non-existing output directory “$opts{dir}”: $!\n" if $opts{dir} && !-d $opts{dir} && !mkdir $opts{dir};

# process SVG...
my $crop = `tempfile`;
chomp $crop;
my(@ids, @temps);
for my $id (qw(normal pressed)) {  # for each of the two button states
  push @ids, $id;

  # render to PNG..
  push @temps, `tempfile`;
  chomp $temps[-1];
  my $cmd = &shell_cmd("inkscape -C -w %d -h %d" . join("", ((map { " -i $_" } @ids), " -j")[0, 1]) . " -e %s %s &>/dev/null",
                       $::COUNTX * $::BUTTON_WIDTH, $::COUNTY * $::BUTTON_HEIGHT, $temps[-1], $opts{file} || $::SVG);
  system $cmd and die "Failed to convert $id buttons vector image to PNG!\n";

  # crop, copy/convert individual cropped PNGs to their respective destinations...
  for my $idx (grep { $::MAPPING{$_} } (0..(($::COUNTX * $::COUNTY) - 1))) {
    # crop...
    $cmd = &shell_cmd("convert -crop %dx%d+%d+%d! %s %s",
                      $::BUTTON_WIDTH, $::BUTTON_HEIGHT, $::BUTTON_WIDTH * ($idx % $::COUNTX), $::BUTTON_HEIGHT * ($idx / $::COUNTX),
                      $temps[-1], "png:$crop");
    system $cmd and die "Failed to crop $id buttons PNG #$idx!\n";

    # copy/convert...
    for (my $target = @temps; $target < @{$::MAPPING{$idx}}; $target += 2) {
      if ($opts{dir}) {
        my $to = "$opts{dir}/$::MAPPING{$idx}[0]" . ($target % 2 ? "" : "_sel") . ".png";
        copy($crop, $to) or warn "Failed to copy $id #$idx to “$to”!\n";
      } else {
        if (-w $::MAPPING{$idx}[$target]) {
          $cmd = &shell_cmd("convert %s -quality 100 %s 2>/dev/null",
                            "png:$crop", $::MAPPING{$idx}[$target]);
          system $cmd and warn "Failed to convert $id #$idx to “$::MAPPING[$idx][$target]”!\n";
        } else {
          warn "Not copying $id #$idx as “$::MAPPING{$idx}[$target]” does not exist or is not writable.\n";
        }
      }
    }
  }
}
push @temps, $crop;
unlink @temps;


#
# subs...
#

sub shell_quote($) {
  local $_ = shift;
  return $_ if /^[-a-z0-9_]+$/i;
  s/'/'\\''/g;
  "'$_'";
}

sub shell_cmd($@) {
  local $_ = shift;
  sprintf $_, map { shell_quote $_ } @_;
}


