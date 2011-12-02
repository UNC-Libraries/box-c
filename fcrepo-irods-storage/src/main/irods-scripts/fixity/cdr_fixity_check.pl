#!/usr/bin/perl

use strict;
use POSIX; # needed for number processing

sub trim($);
sub needsChecksum($$);

my $sixMonthsInSeconds = 15778463; # six months in seconds

my $goodoutputfile = "good_checksums.log";
my $badoutputfile = "bad_checksums.log";

my $startingdirectory = "/cdrZone/home/fedora";

# Change to target iRODS directory
qx{icd $startingdirectory};

# Get the directory tree
my @var = qx{ils -r};  # ils -r

# print @var->[0];

my $currentdirectory = "";

open GOODFILE, ">>", $goodoutputfile or die $!;
open BADFILE, ">>", $badoutputfile or die $!;

# my ($sec,$min,$hour,etc...) = localtime(time);

my $currentTime = time();
my $sixMonthsAgo = $currentTime - $sixMonthsInSeconds;

# For each directory and file
for my $i (0 .. $#var) {
   # print "$i: $var[$i]\n";
    
    # See if it is a stored object (may want to change this check to include the very few system files that don't
    # start with uuid)
    if(trim($var[$i]) =~ m/^(uuid)/i) {
      my $filename = trim($var[$i]);

      print "$currentdirectory/$filename\n";
      
	# get fixity timestamp from object if present


	my $needschecksum = needNewChecksum($currentdirectory, $filename);

        print "needs checksum $needschecksum";

	if($needschecksum) {
	      # Run a checksum on the file
	      my $checksum = `ichksum $currentdirectory/$filename 2>&1`;


	      # Parse the results
      
	      if(trim($checksum) =~ m/^(ERROR)/i) {
		print BADFILE $checksum;	
	      }
	      else {
	        print GOODFILE $checksum;

	  	my $updateAVU = `imeta add -d $currentdirectory/$filename cdrFixity $currentTime`;
	      }
	}

	# update AVU

    #  print "$checksum\n";

    }
    else {
      # directory comes in as 'directorypath:' so we need to remove the ':'
      $currentdirectory = substr(trim($var[$i]), 0, -1);
    }
}


# print $var;

# get cdrFixity AVU and extract current value; return if it has been too long and need to calculate checksum again
sub needNewChecksum($$)
{
       my $currentdirectory = shift;
       my $filename = shift;

	# query for the cdrFixity metadata
        my @fixityAVU = `imeta ls -d $currentdirectory/$filename cdrFixity`;

	my $arraySize = @fixityAVU;

	# see if a value is set; if so, extract it
	if(($arraySize > 2) && ($fixityAVU[2] =~ m/^(attribute)/i)) {
		my @array = split(/\s+/,$fixityAVU[2]);

		$arraySize = @array;

		# parse and check time
		if(($arraySize == 2) && (isdigit(trim(@array[1])))) {
			print @array[1];
			
			if(trim(@array[1]) < $sixMonthsAgo) {
				return 0;
			} else {
				return 1;
			}
		} else {
			print BADFILE "Problem: $currentdirectory\\$filename has cdrFixity of '@array[1]'";
			return 0; # something wrong with cdrFixity value; need to look into problem
		}
	} else {
		return 1; # probably no checksum present, so request one
	}

	return 1; # get a new checksum if we end up here, just in case
}

# Perl trim function to remove whitespace from the start and end of the string
sub trim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
