#!/usr/bin/perl

# Script to clear cdrFixity AVUs
# 
# To run, modify the irods .irodsEnv to make the user fedora
# and connect to iRODS (iinit) and run the script.  It is set
# up to start in the passed in path and clear the 'cdrFixity' AVU.

use strict;

sub trim($);

if ($#ARGV != 0 ) {
	print "usage: clear_cdr_fixity_avus /zoneName/home/fedora\n";
	exit;
}

# my $startingdirectory = "/cdrZone/home/fedora";
my $startingdirectory = $ARGV[0];

# Change to target iRODS directory
qx{icd $startingdirectory};

# Get the directory tree
my @var = qx{ils -r};  # ils -r

# print @var->[0];

my $currentdirectory = "";

# For each directory and file
for my $i (0 .. $#var) {
   # print "$i: $var[$i]\n";
    
    # See if it is a stored object (may want to change this check to include the very few system files that don't
    # start with uuid)
    if(trim($var[$i]) =~ m/^(uuid)/i) {
      my $filename = trim($var[$i]);

      # print "$currentdirectory/$filename\n";

      `imeta rmw -d $currentdirectory/$filename cdrFixity %`;

    }
    else {
      # directory comes in as 'directorypath:' so we need to remove the ':'
      $currentdirectory = substr(trim($var[$i]), 0, -1);
    }
}

# Perl trim function to remove whitespace from the start and end of the string
sub trim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
