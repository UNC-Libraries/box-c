#!/usr/bin/perl

# Script to run replication checks on objects stored in the CDR
# 
# To run, modify the irods .irodsEnv on a client iRODS (not alpha, 
# dev, prod) to make the user fedora and connect to iRODS (iinit) and 
# run the script.  It is set up to start in the passed in path and 
# verify replicas on objects which haven't been checked in 6 months.

use strict;
use POSIX; # needed for number processing

sub trim($);
sub needsReplicaCheck($$);
sub extractResources(@);

# 600000; # 10 minutes for testing 
my $sixMonthsInSeconds =  15778463; # six months in seconds

my $goodoutputfile = "good_replication.log";
my $badoutputfile = "bad_replication.log";

my @requiredResources = ();

if ($#ARGV <= 0) {
	print "usage: cdr_fixity_check /zoneName/home/fedora resource1 resource2\n";
	exit;
}

# extract resources from command line and load into hash
for my $i (1 .. $#ARGV) {
	push(@requiredResources, $ARGV[$i]); 

	print "$ARGV[$i]\n";
}


# my $startingdirectory = "/cdrZone/home/fedora";
my $startingdirectory = $ARGV[0];

# Change to target iRODS directory
qx{icd $startingdirectory};

# Get the directory tree
my @var = qx{ils -r};  # ils -r

# print @var->[0];

my $currentdirectory = "";

open GOODFILE, ">>", $goodoutputfile or die $!;
open BADFILE, ">>", $badoutputfile or die $!;

my $currentTime = time();
my $sixMonthsAgo = $currentTime - $sixMonthsInSeconds;

# For each directory and file
for my $i (0 .. $#var) {
    
    # See if it is a stored object (may want to change this check to include the very few system files that don't
    # start with uuid)
    if(trim($var[$i]) =~ m/^(uuid)/i) 
    {
      my $filename = trim($var[$i]);

      # print "$currentdirectory/$filename\n";
      
	# check to see if this file needs a replication check at this time
	my $needsCheck = needsReplicaCheck($currentdirectory, $filename);

	if($needsCheck) 
	{
	      # Get the resources of the file
	      my @lsListing = `ils -l $currentdirectory/$filename 2>&1`;
     
		if(trim($lsListing[0]) =~ m/^(ERROR)/i) 
		{
			print BADFILE @lsListing;	
		}
		else 
		{		
			# parse ils result and extract resources

			# print "@lsListing\n";

			my %fileResources = extractResources(@lsListing);

			my $missingResource = 0;

			# check that file has each required resource
			for my $i (0..$#requiredResources) 
			{
				if(! exists $fileResources{$requiredResources[$i]}) 
				{
					$missingResource = 1;
					print BADFILE "$currentdirectory/$filename is has not been replicated to $requiredResources[$i]\n";
				}
			}

			if(!$missingResource) 
			{
		        	print GOODFILE @lsListing;
	
		  		my $updateAVU = `imeta add -d $currentdirectory/$filename cdrReplica $currentTime`;
			}
		}
	}
    }
    else {
      # directory comes in as 'directorypath:' so we need to remove the ':'
      $currentdirectory = substr(trim($var[$i]), 0, -1);
    }
}

sub extractResources(@) 
{
	my @lsListing = shift;

	my %fileResources = ();

	for my $i (0 .. $#lsListing) {	
		my @array = split(/\s+/,$lsListing[$i]);

		# print "$array[0] v $array[1] v $array[2] v $array[3]\n";

		$fileResources{$array[3]} = $array[3];
	}

	return %fileResources;
}


# get cdrReplica AVU and extract current value; return if it has been too long and need to check replicas again
sub needsReplicaCheck($$)
{
       my $currentdirectory = shift;
       my $filename = shift;

	# query for the cdrReplica metadata
        my @aVU = `imeta ls -d $currentdirectory/$filename cdrReplica`;

	my $arraySize = @aVU;

	# see if a value is set; if so, extract it
	if(($arraySize > 2) && ($aVU[2] =~ m/^(value)/i)) {
		my @array = split(/\s+/,$aVU[2]);

		$arraySize = @array;

		# parse and check time
		if(($arraySize == 2) && (isdigit(trim(@array[1])))) {
			
			if(trim(@array[1]) > $sixMonthsAgo) { # don't do a replica check at this time

				return 0;
			} else {
				# remove old AVU value; otherwise they accumulate

				`imeta rmw -d $currentdirectory/$filename cdrReplica %`;

				return 1; # perform a new replica check
			}
		} else {

			print BADFILE "Problem: $currentdirectory\\$filename has cdrReplica of '@array[1]'";
			return 0; # something wrong with cdrReplica value; need to look into problem
		}
	} else {

		return 1; # probably no cdrReplica present, so request one
	}


	return 1; # get a new replica check if we end up here, just in case
}

# Perl trim function to remove whitespace from the start and end of the string
sub trim($)
{
	my $string = shift;
	$string =~ s/^\s+//;
	$string =~ s/\s+$//;
	return $string;
}
